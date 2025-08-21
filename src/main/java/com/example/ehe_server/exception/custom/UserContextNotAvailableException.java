package com.example.ehe_server.exception.custom;

public class UserContextNotAvailableException extends RuntimeException {
    public UserContextNotAvailableException(String message) {
        super(message);
    }
}
