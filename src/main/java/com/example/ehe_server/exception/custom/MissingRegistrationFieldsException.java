package com.example.ehe_server.exception.custom;

public class MissingRegistrationFieldsException extends ValidationException {
    public MissingRegistrationFieldsException() {
        super("error.message.missingRegistrationFields", "error.logDetail.missingRegistrationFields");
    }
}