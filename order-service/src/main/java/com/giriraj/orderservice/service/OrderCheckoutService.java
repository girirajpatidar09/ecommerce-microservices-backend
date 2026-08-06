package com.giriraj.orderservice.service;

import com.giriraj.orderservice.dto.CheckoutResponseDTO;

public interface OrderCheckoutService {

    CheckoutResponseDTO placeOrder(Long userId);
}
