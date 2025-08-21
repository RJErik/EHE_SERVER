package com.example.ehe_server.exception.custom;

public class InvalidCredentialsException extends AuthorizationException {
    public InvalidCredentialsException(String email) {
        super("error.message.invalidCredentials", "error.logDetail.invalidCredentials", email);
    }
}