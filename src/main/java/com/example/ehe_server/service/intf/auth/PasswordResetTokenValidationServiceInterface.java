package com.example.ehe_server.service.intf.auth;

public interface PasswordResetTokenValidationServiceInterface {
    void validatePasswordResetToken(String token);
}
