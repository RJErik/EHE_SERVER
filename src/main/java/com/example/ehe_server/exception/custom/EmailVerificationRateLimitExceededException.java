package com.example.ehe_server.exception.custom;

public class EmailVerificationRateLimitExceededException extends RuntimeException {
    public EmailVerificationRateLimitExceededException(String message) {
        super(message);
    }
}
