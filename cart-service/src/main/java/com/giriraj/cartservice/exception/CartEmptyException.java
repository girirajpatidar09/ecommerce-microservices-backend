package com.giriraj.cartservice.exception;

public class CartEmptyException
        extends RuntimeException {

    public CartEmptyException(String message) {
        super(message);
    }
}