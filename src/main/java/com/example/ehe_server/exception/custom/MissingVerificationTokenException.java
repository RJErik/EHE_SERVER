package com.example.ehe_server.exception.custom;

public class MissingVerificationTokenException extends ValidationException {
    public MissingVerificationTokenException() {
        super("error.message.missingVerificationToken", "error.logDetail.missingVerificationToken");
    }
}