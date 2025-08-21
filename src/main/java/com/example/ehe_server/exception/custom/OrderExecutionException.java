package com.example.ehe_server.exception.custom;

public class OrderExecutionException extends RuntimeException {
    public OrderExecutionException(String message) {
        super(message);
    }
}
