package com.giriraj.orderservice.service;

import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.PaymentVerificationRequestDTO;

public interface OrderPaymentService {

    OrderResponseDTO verifyPayment(
            Long userId,
            PaymentVerificationRequestDTO request
    );
}
