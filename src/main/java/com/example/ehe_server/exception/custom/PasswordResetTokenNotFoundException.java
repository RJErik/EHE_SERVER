package com.example.ehe_server.exception.custom;

public class PasswordResetTokenNotFoundException extends ResourceNotFoundException {
    public PasswordResetTokenNotFoundException(String token) {
        super("error.message.passwordResetTokenNotFound", "error.logDetail.passwordResetTokenNotFound", token);
    }
}