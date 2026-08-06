package com.giriraj.orderservice.service.impl;

import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.remote.StockItemRequestDTO;
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
import com.giriraj.orderservice.service.OrderPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPersistenceServiceImpl
        implements OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public Long createPendingOrder(Long userId) {

        Order order = new Order();

        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        Order savedOrder =
                orderRepository.save(order);

        return savedOrder.getId();
    }

    @Override
    @Transactional
    public OrderResponseDTO preparePendingOrder(
            Long orderId,
            StockReservationResponseDTO reservation
    ) {

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        if (order.getStatus()
                != OrderStatus.PENDING) {

            throw new InvalidOrderStateException(
                    "Only pending Order can be prepared"
            );
        }

        if (reservation == null
                || reservation.getItems() == null
                || reservation.getItems().isEmpty()) {

            throw new InvalidOrderStateException(
                    "Cannot prepare Order without items"
            );
        }

        if (!order.getItems().isEmpty()) {
            throw new InvalidOrderStateException(
                    "Order items have already been prepared"
            );
        }

        BigDecimal calculatedTotal =
                BigDecimal.ZERO;

        for (StockReservationItemDTO reservedItem
                : reservation.getItems()) {

            OrderItem orderItem =
                    new OrderItem();

            orderItem.setProductId(
                    reservedItem.getProductId()
            );

            orderItem.setProductName(
                    reservedItem.getProductName()
            );

            orderItem.setImageUrl(
                    reservedItem.getImageUrl()
            );

            orderItem.setQuantity(
                    reservedItem.getQuantity()
            );

            orderItem.setUnitPrice(
                    reservedItem.getUnitPrice()
            );

            order.addItem(orderItem);

            BigDecimal itemTotal =
                    reservedItem.getUnitPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            reservedItem
                                                    .getQuantity()
                                    )
                            );

            calculatedTotal =
                    calculatedTotal.add(
                            itemTotal
                    );
        }

        order.setTotalAmount(
                calculatedTotal
        );

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmPaidOrder(
            Long userId,
            Long orderId
    ) {

        Order order = orderRepository
                .findByIdAndUserIdForUpdate(
                        orderId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        if (order.getStatus()
                != OrderStatus.PENDING) {

            throw new InvalidOrderStateException(
                    "Only pending Order can be confirmed"
            );
        }

        if (order.getItems() == null
                || order.getItems().isEmpty()) {

            throw new InvalidOrderStateException(
                    "Order cannot be confirmed without items"
            );
        }

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void markOrderFailed(Long orderId) {

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        if (order.getStatus()
                == OrderStatus.PENDING) {

            order.setStatus(
                    OrderStatus.FAILED
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUser(
            Long userId
    ) {

        return orderRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(
            Long userId,
            Long orderId
    ) {

        Order order = orderRepository
                .findByIdAndUserId(
                        orderId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void markCompensationPending(
            Long orderId
    ) {

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        order.setStatus(
                OrderStatus.COMPENSATION_PENDING
        );
    }

    @Override
    @Transactional
    public StockRequestDTO beginCancellation(
            Long userId,
            Long orderId
    ) {

        Order order = orderRepository
                .findByIdAndUserIdForUpdate(
                        orderId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        if (order.getStatus()
                == OrderStatus.CANCELLED) {

            throw new InvalidOrderStateException(
                    "Order is already cancelled"
            );
        }

        if (order.getStatus()
                == OrderStatus.CANCELLATION_PENDING) {

            throw new InvalidOrderStateException(
                    "Order cancellation is already in progress"
            );
        }

        if (order.getStatus()
                == OrderStatus.SHIPPED
                || order.getStatus()
                == OrderStatus.DELIVERED) {

            throw new InvalidOrderStateException(
                    "Shipped or delivered Orders cannot be cancelled"
            );
        }

        if (order.getStatus()
                != OrderStatus.CONFIRMED) {

            throw new InvalidOrderStateException(
                    "Only confirmed Orders can be cancelled"
            );
        }

        List<StockItemRequestDTO> stockItems =
                order.getItems()
                        .stream()
                        .map(orderItem ->
                                new StockItemRequestDTO(
                                        orderItem.getProductId(),
                                        orderItem.getQuantity()
                                )
                        )
                        .toList();

        order.setStatus(
                OrderStatus.CANCELLATION_PENDING
        );

        return new StockRequestDTO(stockItems);
    }

    @Override
    @Transactional
    public void completeCancellation(
            Long orderId
    ) {

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with id: "
                                        + orderId
                        )
                );

        if (order.getStatus()
                != OrderStatus.CANCELLATION_PENDING) {

            throw new InvalidOrderStateException(
                    "Order is not awaiting cancellation"
            );
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );
    }
}
