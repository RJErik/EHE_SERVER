package com.example.ehe_server.exception.custom;

public class InvalidTransactionTypeException extends ValidationException {
    public InvalidTransactionTypeException(String type) {
        super("error.message.invalidTransactionType", "error.logDetail.invalidTransactionType", type);
    }
}