package com.giriraj.orderservice.service;

import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.dto.remote.StockReservationItemDTO;
import com.giriraj.orderservice.dto.remote.StockReservationResponseDTO;
import com.giriraj.orderservice.entity.Order;
import com.giriraj.orderservice.entity.OrderItem;
import com.giriraj.orderservice.enums.OrderStatus;
import com.giriraj.orderservice.exception.InvalidOrderStateException;
import com.giriraj.orderservice.exception.ResourceNotFoundException;
import com.giriraj.orderservice.mapper.OrderMapper;
import com.giriraj.orderservice.repository.OrderRepository;
import com.giriraj.orderservice.service.impl.OrderPersistenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderPersistenceServiceImpl persistenceService;

    @Test
    void createPendingOrder_shouldSavePendingOrder() {

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(100L);

                    return order;
                });

        Long orderId =
                persistenceService.createPendingOrder(1L);

        assertEquals(100L, orderId);

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();

        assertEquals(1L, savedOrder.getUserId());
        assertEquals(
                OrderStatus.PENDING,
                savedOrder.getStatus()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        savedOrder.getTotalAmount()
                )
        );
    }

    @Test
    void confirmOrder_shouldCreateHistoricalSnapshots() {

        Order order = pendingOrder();

        when(orderRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        StockReservationItemDTO reservedItem =
                new StockReservationItemDTO(
                        10L,
                        "Test Laptop",
                        "https://example.com/laptop.jpg",
                        2,
                        new BigDecimal("1000.00"),
                        new BigDecimal("2000.00")
                );

        StockReservationResponseDTO reservation =
                new StockReservationResponseDTO(
                        java.util.List.of(reservedItem),
                        new BigDecimal("2000.00")
                );

        OrderResponseDTO expectedResponse =
                new OrderResponseDTO();

        expectedResponse.setId(100L);
        expectedResponse.setStatus(
                OrderStatus.CONFIRMED
        );

        when(orderMapper.toResponse(order))
                .thenReturn(expectedResponse);

        OrderResponseDTO result =
                persistenceService.confirmOrder(
                        100L,
                        reservation
                );

        assertSame(expectedResponse, result);
        assertEquals(
                OrderStatus.CONFIRMED,
                order.getStatus()
        );

        assertEquals(1, order.getItems().size());

        OrderItem orderItem =
                order.getItems().get(0);

        assertSame(order, orderItem.getOrder());
        assertEquals(10L, orderItem.getProductId());
        assertEquals(
                "Test Laptop",
                orderItem.getProductName()
        );
        assertEquals(
                "https://example.com/laptop.jpg",
                orderItem.getImageUrl()
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(
                                orderItem.getUnitPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(
                                order.getTotalAmount()
                        )
        );
    }

    @Test
    void confirmOrder_whenOrderIsNotPending_shouldThrow() {

        Order order = pendingOrder();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        StockReservationResponseDTO reservation =
                new StockReservationResponseDTO(
                        java.util.List.of(
                                new StockReservationItemDTO(
                                        10L,
                                        "Product",
                                        null,
                                        1,
                                        new BigDecimal("100.00"),
                                        new BigDecimal("100.00")
                                )
                        ),
                        new BigDecimal("100.00")
                );

        assertThrows(
                InvalidOrderStateException.class,
                () -> persistenceService.confirmOrder(
                        100L,
                        reservation
                )
        );
    }

    @Test
    void markOrderFailed_shouldChangeOnlyPendingOrder() {

        Order order = pendingOrder();

        when(orderRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        persistenceService.markOrderFailed(100L);

        assertEquals(
                OrderStatus.FAILED,
                order.getStatus()
        );
    }

    @Test
    void beginCancellation_shouldCreateStockRestoreRequest() {

        Order order = pendingOrder();
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem orderItem = new OrderItem();

        orderItem.setProductId(10L);
        orderItem.setProductName("Test Laptop");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(
                new BigDecimal("1000.00")
        );

        order.addItem(orderItem);

        when(orderRepository
                .findByIdAndUserIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(order));

        StockRequestDTO stockRequest =
                persistenceService.beginCancellation(
                        1L,
                        100L
                );

        assertEquals(
                OrderStatus.CANCELLATION_PENDING,
                order.getStatus()
        );

        assertEquals(1, stockRequest.getItems().size());
        assertEquals(
                10L,
                stockRequest.getItems()
                        .get(0)
                        .getProductId()
        );
        assertEquals(
                2,
                stockRequest.getItems()
                        .get(0)
                        .getQuantity()
        );
    }

    @Test
    void beginCancellation_whenOrderIsShipped_shouldThrow() {

        Order order = pendingOrder();
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository
                .findByIdAndUserIdForUpdate(100L, 1L))
                .thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStateException.class,
                () -> persistenceService.beginCancellation(
                        1L,
                        100L
                )
        );

        assertEquals(
                OrderStatus.SHIPPED,
                order.getStatus()
        );
    }

    @Test
    void getOrderById_whenOwnedByAnotherUser_shouldReturnNotFound() {

        when(orderRepository.findByIdAndUserId(
                100L,
                2L
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> persistenceService.getOrderById(
                        2L,
                        100L
                )
        );

        verifyNoInteractions(orderMapper);
    }

    private Order pendingOrder() {

        Order order = new Order();

        order.setId(100L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        return order;
    }
}