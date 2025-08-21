package com.example.ehe_server.exception.custom;

public class UnauthorizedAutomatedTradeRuleAccessException extends RuntimeException {
    public UnauthorizedAutomatedTradeRuleAccessException(String message) {
        super(message);
    }
}
