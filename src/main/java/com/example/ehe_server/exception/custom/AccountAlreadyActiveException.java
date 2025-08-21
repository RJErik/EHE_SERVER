package com.example.ehe_server.exception.custom;

public class AccountAlreadyActiveException extends BusinessRuleException {
    public AccountAlreadyActiveException(String email) {
        super("error.message.accountAlreadyActive", "error.logDetail.accountAlreadyActive", email);
    }
}