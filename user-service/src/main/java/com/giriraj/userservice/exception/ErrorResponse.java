package com.giriraj.userservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor

public class ErrorResponse {

    private boolean success;
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}