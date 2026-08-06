package com.giriraj.orderservice.service;

public interface OrderCancellationService {

    void cancelOrder(
            Long userId,
            Long orderId
    );
}