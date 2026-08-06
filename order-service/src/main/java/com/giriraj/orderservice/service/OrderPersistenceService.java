package com.giriraj.orderservice.service;

import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.dto.remote.StockReservationResponseDTO;

import java.util.List;

public interface OrderPersistenceService {

    Long createPendingOrder(Long userId);

    OrderResponseDTO preparePendingOrder(
            Long orderId,
            StockReservationResponseDTO reservation
    );

    OrderResponseDTO confirmPaidOrder(
            Long userId,
            Long orderId
    );

    void markOrderFailed(Long orderId);

    List<OrderResponseDTO> getOrdersByUser(
            Long userId
    );

    OrderResponseDTO getOrderById(
            Long userId,
            Long orderId
    );

    void markCompensationPending(Long orderId);

    StockRequestDTO beginCancellation(
            Long userId,
            Long orderId
    );

    void completeCancellation(Long orderId);
}
