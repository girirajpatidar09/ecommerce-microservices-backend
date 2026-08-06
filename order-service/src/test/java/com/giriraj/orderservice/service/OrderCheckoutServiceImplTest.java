package com.giriraj.orderservice.service;

import com.giriraj.orderservice.client.CartClient;
import com.giriraj.orderservice.client.PaymentClient;
import com.giriraj.orderservice.client.ProductStockClient;
import com.giriraj.orderservice.client.UserClient;
import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.remote.*;
import com.giriraj.orderservice.enums.OrderStatus;
import com.giriraj.orderservice.exception.CompensationFailedException;
import com.giriraj.orderservice.exception.ExternalServiceException;
import com.giriraj.orderservice.service.impl.OrderCheckoutServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private ProductStockClient productStockClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OrderPersistenceService persistenceService;

    @InjectMocks
    private OrderCheckoutServiceImpl checkoutService;

    private CheckoutCartDTO checkoutCart;
    private StockReservationResponseDTO reservation;
    private OrderResponseDTO confirmedOrder;

    @BeforeEach
    void setUp() {

        checkoutCart = new CheckoutCartDTO(
                1L,
                List.of(
                        new CheckoutCartItemDTO(
                                10L,
                                2
                        )
                )
        );

        reservation =
                new StockReservationResponseDTO(
                        List.of(
                                new StockReservationItemDTO(
                                        10L,
                                        "Test Laptop",
                                        "https://example.com/laptop.jpg",
                                        2,
                                        new BigDecimal("1000.00"),
                                        new BigDecimal("2000.00")
                                )
                        ),
                        new BigDecimal("2000.00")
                );

        confirmedOrder = new OrderResponseDTO();

        confirmedOrder.setId(100L);
        confirmedOrder.setStatus(
                OrderStatus.CONFIRMED
        );
        confirmedOrder.setTotalAmount(
                new BigDecimal("2000.00")
        );
    }

    @Test
    void placeOrder_shouldProcessPaymentConfirmOrderAndClearCart() {

        stubValidUserAndCart();
        stubSuccessfulReservation();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        when(persistenceService.confirmOrder(
                100L,
                reservation
        )).thenReturn(confirmedOrder);

        OrderResponseDTO result =
                checkoutService.placeOrder(1L);

        assertSame(confirmedOrder, result);

        ArgumentCaptor<PaymentRequestDTO> paymentCaptor =
                ArgumentCaptor.forClass(
                        PaymentRequestDTO.class
                );

        verify(paymentClient)
                .processPayment(paymentCaptor.capture());

        PaymentRequestDTO paymentRequest =
                paymentCaptor.getValue();

        assertEquals(
                100L,
                paymentRequest.getOrderId()
        );

        assertEquals(
                1L,
                paymentRequest.getUserId()
        );

        assertEquals(
                new BigDecimal("2000.00"),
                paymentRequest.getAmount()
        );

        ArgumentCaptor<StockRequestDTO> stockCaptor =
                ArgumentCaptor.forClass(
                        StockRequestDTO.class
                );

        verify(productStockClient)
                .reserveStock(stockCaptor.capture());

        StockRequestDTO stockRequest =
                stockCaptor.getValue();

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

        verify(persistenceService)
                .createPendingOrder(1L);

        verify(persistenceService)
                .confirmOrder(100L, reservation);

        verify(cartClient)
                .clearCheckoutCart(1L);

        verify(productStockClient, never())
                .restoreStock(any());

        verify(persistenceService, never())
                .markOrderFailed(anyLong());
    }

    @Test
    void placeOrder_whenPaymentFails_shouldRestoreStockAndMarkOrderFailed() {

        stubValidUserAndCart();
        stubSuccessfulReservation();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        FeignException paymentFailure =
                mock(FeignException.class);

        doThrow(paymentFailure)
                .when(paymentClient)
                .processPayment(
                        any(PaymentRequestDTO.class)
                );

        assertThrows(
                ExternalServiceException.class,
                () -> checkoutService.placeOrder(1L)
        );

        verify(productStockClient)
                .restoreStock(
                        any(StockRequestDTO.class)
                );

        verify(persistenceService)
                .markOrderFailed(100L);

        verify(persistenceService, never())
                .confirmOrder(anyLong(), any());

        verify(cartClient, never())
                .clearCheckoutCart(anyLong());
    }

    @Test
    void placeOrder_whenConfirmationFailsAfterPayment_shouldMarkCompensationPending() {

        stubValidUserAndCart();
        stubSuccessfulReservation();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        when(persistenceService.confirmOrder(
                100L,
                reservation
        )).thenThrow(
                new RuntimeException(
                        "Order database failure"
                )
        );

        assertThrows(
                CompensationFailedException.class,
                () -> checkoutService.placeOrder(1L)
        );

        verify(paymentClient)
                .processPayment(
                        any(PaymentRequestDTO.class)
                );

        verify(persistenceService)
                .markCompensationPending(100L);

        verify(productStockClient, never())
                .restoreStock(any());

        verify(persistenceService, never())
                .markOrderFailed(anyLong());

        verify(cartClient, never())
                .clearCheckoutCart(anyLong());
    }

    @Test
    void placeOrder_whenReservationResponseIsInvalid_shouldMarkFailedWithoutPayment() {

        stubValidUserAndCart();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        when(productStockClient.reserveStock(
                any(StockRequestDTO.class)
        )).thenReturn(null);

        assertThrows(
                ExternalServiceException.class,
                () -> checkoutService.placeOrder(1L)
        );

        verify(persistenceService)
                .markOrderFailed(100L);

        verifyNoInteractions(paymentClient);

        verify(productStockClient, never())
                .restoreStock(any());

        verify(persistenceService, never())
                .confirmOrder(anyLong(), any());
    }

    @Test
    void placeOrder_whenStockRestorationFails_shouldMarkCompensationPending() {

        stubValidUserAndCart();
        stubSuccessfulReservation();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        FeignException paymentFailure =
                mock(FeignException.class);

        doThrow(paymentFailure)
                .when(paymentClient)
                .processPayment(
                        any(PaymentRequestDTO.class)
                );

        FeignException restorationFailure =
                mock(FeignException.class);

        doThrow(restorationFailure)
                .when(productStockClient)
                .restoreStock(
                        any(StockRequestDTO.class)
                );

        assertThrows(
                CompensationFailedException.class,
                () -> checkoutService.placeOrder(1L)
        );

        verify(persistenceService)
                .markCompensationPending(100L);

        verify(persistenceService, never())
                .markOrderFailed(100L);
    }

    @Test
    void placeOrder_whenCartCleanupFails_shouldStillReturnConfirmedOrder() {

        stubValidUserAndCart();
        stubSuccessfulReservation();

        when(persistenceService.createPendingOrder(1L))
                .thenReturn(100L);

        when(persistenceService.confirmOrder(
                100L,
                reservation
        )).thenReturn(confirmedOrder);

        FeignException cleanupFailure =
                mock(FeignException.class);

        doThrow(cleanupFailure)
                .when(cartClient)
                .clearCheckoutCart(1L);

        OrderResponseDTO result =
                checkoutService.placeOrder(1L);

        assertSame(confirmedOrder, result);

        verify(paymentClient)
                .processPayment(
                        any(PaymentRequestDTO.class)
                );

        verify(productStockClient, never())
                .restoreStock(any());

        verify(persistenceService, never())
                .markOrderFailed(anyLong());

        verify(persistenceService, never())
                .markCompensationPending(anyLong());
    }

    @Test
    void placeOrder_whenUserResponseIsInvalid_shouldNotCreateOrder() {

        when(userClient.getUserById(1L))
                .thenReturn(null);

        assertThrows(
                ExternalServiceException.class,
                () -> checkoutService.placeOrder(1L)
        );

        verifyNoInteractions(cartClient);
        verifyNoInteractions(productStockClient);
        verifyNoInteractions(paymentClient);
        verifyNoInteractions(persistenceService);
    }

    private void stubValidUserAndCart() {

        ApiResponse<UserSummaryDTO> userResponse =
                new ApiResponse<>(
                        true,
                        "User fetched successfully",
                        new UserSummaryDTO(1L),
                        LocalDateTime.now()
                );

        ApiResponse<CheckoutCartDTO> cartResponse =
                new ApiResponse<>(
                        true,
                        "Cart fetched successfully",
                        checkoutCart,
                        LocalDateTime.now()
                );

        when(userClient.getUserById(1L))
                .thenReturn(userResponse);

        when(cartClient.getCheckoutCart(1L))
                .thenReturn(cartResponse);
    }

    private void stubSuccessfulReservation() {

        ApiResponse<StockReservationResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Stock reserved successfully",
                        reservation,
                        LocalDateTime.now()
                );

        when(productStockClient.reserveStock(
                any(StockRequestDTO.class)
        )).thenReturn(response);
    }
}
