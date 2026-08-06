package com.giriraj.orderservice.dto;

import com.giriraj.orderservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {

    private Long orderId;

    private BigDecimal amount;

    private OrderStatus orderStatus;

    private String paymentStatus;

    private String currency;

    private String razorpayKeyId;

    private String razorpayOrderId;
}
