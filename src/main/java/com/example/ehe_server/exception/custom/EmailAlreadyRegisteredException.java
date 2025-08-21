package com.example.ehe_server.exception.custom;

public class EmailAlreadyRegisteredException extends BusinessRuleException {
    public EmailAlreadyRegisteredException(String email) {
        super("error.message.emailAlreadyRegistered", "error.logDetail.emailAlreadyRegistered", email);
    }
}