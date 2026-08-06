package com.giriraj.paymentservice.controller;

import com.giriraj.paymentservice.dto.PaymentRequestDTO;
import com.giriraj.paymentservice.dto.PaymentResponseDTO;
import com.giriraj.paymentservice.dto.PaymentVerificationRequestDTO;
import com.giriraj.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request
    ) {
        PaymentResponseDTO response =
                paymentService.processPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDTO> verifyPayment(
            @Valid @RequestBody
            PaymentVerificationRequestDTO request
    ) {
        PaymentResponseDTO response =
                paymentService.verifyPayment(request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/orders/{orderId}/refund")
    public ResponseEntity<PaymentResponseDTO> refundPayment(
            @PathVariable Long orderId
    ) {
        PaymentResponseDTO response =
                paymentService.refundPayment(orderId);

        return ResponseEntity.ok(response);
    }
}
