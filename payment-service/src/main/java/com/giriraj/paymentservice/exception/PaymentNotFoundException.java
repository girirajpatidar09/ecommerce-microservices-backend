package com.giriraj.paymentservice.exception;

public class PaymentNotFoundException
        extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
