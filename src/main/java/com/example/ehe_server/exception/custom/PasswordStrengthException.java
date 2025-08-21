package com.example.ehe_server.exception.custom;

public class PasswordStrengthException extends ValidationException {
    public PasswordStrengthException() {
        super("error.message.PASSWORD_TOO_WEAK", "error.logDetail.PASSWORD_TOO_WEAK");
    }
}
