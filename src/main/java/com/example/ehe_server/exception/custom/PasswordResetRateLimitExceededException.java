package com.example.ehe_server.exception.custom;

public class PasswordResetRateLimitExceededException extends RuntimeException {
    public PasswordResetRateLimitExceededException(String message) {
        super(message);
    }
}
