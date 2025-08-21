package com.example.ehe_server.exception.custom;

public class UserAccountNotActiveException extends RuntimeException {
    public UserAccountNotActiveException(String message) {
        super(message);
    }
}
