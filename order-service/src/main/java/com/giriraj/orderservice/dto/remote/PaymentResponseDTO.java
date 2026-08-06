package com.giriraj.orderservice.dto.remote;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentResponseDTO {

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String status;

    private String currency;

    private String razorpayKeyId;

    private String razorpayOrderId;
}
