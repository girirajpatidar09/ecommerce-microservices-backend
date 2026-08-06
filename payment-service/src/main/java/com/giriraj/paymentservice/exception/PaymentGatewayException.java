package com.giriraj.paymentservice.exception;

public class PaymentGatewayException
        extends RuntimeException {

    public PaymentGatewayException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
