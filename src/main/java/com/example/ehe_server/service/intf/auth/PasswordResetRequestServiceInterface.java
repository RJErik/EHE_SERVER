package com.example.ehe_server.service.intf.auth;

public interface PasswordResetRequestServiceInterface {
    void requestPasswordResetForUnauthenticatedUser(String email);
    void requestPasswordResetForAuthenticatedUser(Integer userId);
}