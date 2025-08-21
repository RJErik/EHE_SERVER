package com.example.ehe_server.exception.custom;

public class PortfolioNotFoundOrUnauthorizedException extends RuntimeException {
    public PortfolioNotFoundOrUnauthorizedException(String message) {
        super(message);
    }
}
