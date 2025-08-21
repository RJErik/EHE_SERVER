package com.example.ehe_server.exception.custom;

public class NoApiKeyForPortfolioException extends RuntimeException {
    public NoApiKeyForPortfolioException(String message) {
        super(message);
    }
}
