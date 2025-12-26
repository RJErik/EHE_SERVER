package com.example.ehe_server.exception.custom;

public class InvalidAccountStatusException extends ValidationException {
    public InvalidAccountStatusException(String status) {
        super("error.message.invalidAccountStatus", "error.logDetail.invalidAccountStatus", status);
    }
}