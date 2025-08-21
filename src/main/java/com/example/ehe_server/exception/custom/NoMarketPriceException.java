package com.example.ehe_server.exception.custom;

public class NoMarketPriceException extends RuntimeException {
    public NoMarketPriceException(String message) {
        super(message);
    }
}
