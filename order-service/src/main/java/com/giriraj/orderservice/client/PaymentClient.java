package com.giriraj.orderservice.client;

import com.giriraj.orderservice.dto.PaymentVerificationRequestDTO;
import com.giriraj.orderservice.dto.remote.PaymentRequestDTO;
import com.giriraj.orderservice.dto.remote.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service",
        path = "/api/payments"
)
public interface PaymentClient {

    @PostMapping
    PaymentResponseDTO processPayment(
            @RequestBody PaymentRequestDTO request
    );

    @PostMapping("/verify")
    PaymentResponseDTO verifyPayment(
            @RequestBody PaymentVerificationRequestDTO request
    );

    @PutMapping("/orders/{orderId}/refund")
    void refundPayment(
            @PathVariable("orderId") Long orderId
    );
}
