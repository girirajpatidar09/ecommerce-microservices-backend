package com.giriraj.cartservice.exception;

public class ExternalServiceException
        extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }
}