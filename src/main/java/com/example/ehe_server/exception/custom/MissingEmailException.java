package com.example.ehe_server.exception.custom;

public class MissingEmailException extends ValidationException {
    public MissingEmailException() {
        super("error.message.missingEmail", "error.logDetail.missingEmail");
    }
}