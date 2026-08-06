package com.giriraj.paymentservice.service;

import com.giriraj.paymentservice.dto.PaymentRequestDTO;
import com.giriraj.paymentservice.dto.PaymentResponseDTO;
import com.giriraj.paymentservice.dto.PaymentVerificationRequestDTO;

public interface PaymentService {

    PaymentResponseDTO processPayment(
            PaymentRequestDTO request
    );

    PaymentResponseDTO verifyPayment(
            PaymentVerificationRequestDTO request
    );

    PaymentResponseDTO refundPayment(
            Long orderId
    );
}
