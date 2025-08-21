package com.example.ehe_server.exception.custom;

public class VerificationTokenStatusException extends RuntimeException {
    public VerificationTokenStatusException(String message) {
        super(message);
    }
}
