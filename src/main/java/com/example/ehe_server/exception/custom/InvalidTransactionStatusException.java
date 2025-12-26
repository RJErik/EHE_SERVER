package com.example.ehe_server.exception.custom;

public class InvalidTransactionStatusException extends ValidationException {
    public InvalidTransactionStatusException(String status) {
        super("error.message.invalidTransactionStatus", "error.logDetail.invalidTransactionStatus", status);
    }
}