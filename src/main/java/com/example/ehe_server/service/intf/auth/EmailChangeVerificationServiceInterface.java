package com.example.ehe_server.service.intf.auth;

public interface EmailChangeVerificationServiceInterface {
    /**
     * Verifies an email change token and updates the user's email
     * @param token The verification token
     * @return A map containing the result of the operation
     */
    void validateEmailChange(String token);
}
