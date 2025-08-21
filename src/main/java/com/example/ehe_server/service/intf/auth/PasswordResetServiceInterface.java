package com.example.ehe_server.service.intf.auth;

public interface PasswordResetServiceInterface {
    void resetPassword(String token, String newPassword);
}
