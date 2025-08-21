package com.example.ehe_server.exception.custom;

public class InvalidUserIdentifierException extends RuntimeException {
    public InvalidUserIdentifierException(String message) {
        super(message);
    }
}
