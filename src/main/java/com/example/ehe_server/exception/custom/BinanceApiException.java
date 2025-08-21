package com.example.ehe_server.exception.custom;

public class BinanceApiException extends RuntimeException {
    public BinanceApiException(String message) {
        super(message);
    }
}
