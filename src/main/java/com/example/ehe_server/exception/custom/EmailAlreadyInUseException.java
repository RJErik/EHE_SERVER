package com.example.ehe_server.exception.custom;

public class EmailAlreadyInUseException extends BusinessRuleException {
    public EmailAlreadyInUseException(String newEmail) {
        super("error.message.emailAlreadyInUse", "error.logDetail.emailAlreadyInUse", newEmail);
    }
}