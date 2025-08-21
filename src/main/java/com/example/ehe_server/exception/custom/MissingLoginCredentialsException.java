package com.example.ehe_server.exception.custom;

public class MissingLoginCredentialsException extends ValidationException {
    public MissingLoginCredentialsException() {
        super("error.message.missingLoginCredentials", "error.logDetail.missingLoginCredentials");
    }
}