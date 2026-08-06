package com.giriraj.userservice.exception;

public class PhoneAlreadyExistsException
        extends RuntimeException {

    public PhoneAlreadyExistsException(String message) {
        super(message);
    }
}