package com.giriraj.paymentservice.dto;

import com.giriraj.paymentservice.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponseDTO {

    private Long id;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private String razorpayKeyId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpayRefundId;

    private PaymentStatus status;

    private LocalDateTime createdAt;

    public PaymentResponseDTO(
            Long id,
            Long orderId,
            Long userId,
            BigDecimal amount,
            PaymentStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
