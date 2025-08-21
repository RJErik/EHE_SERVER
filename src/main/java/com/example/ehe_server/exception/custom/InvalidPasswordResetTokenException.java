package com.example.ehe_server.exception.custom;

public class InvalidPasswordResetTokenException extends ResourceNotFoundException {
    public InvalidPasswordResetTokenException(String token) {
        super("error.message.invalidPasswordResetToken", "error.logDetail.invalidPasswordResetToken", token);
    }
}