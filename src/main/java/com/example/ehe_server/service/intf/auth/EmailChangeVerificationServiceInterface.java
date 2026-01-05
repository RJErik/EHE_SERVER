package com.example.ehe_server.service.intf.auth;

public interface EmailChangeVerificationServiceInterface {
    void validateEmailChange(String token);
}
