package com.example.ehe_server.service.intf.auth;

import java.util.Map;

public interface VerificationServiceInterface {

    /**
     * Handles the request to resend a verification email for a given email address.
     * Includes rate limiting and invalidation of previous tokens.
     *
     * @param email The email address of the user requesting the resend.
     * @return A map containing success status, message, and optionally rate limit info.
     */
    Map<String, Object> resendVerification(String email);

    /**
     * Verifies a user's account using the provided token.
     * Updates the token status and user status upon successful verification.
     * Implement this method fully later.
     *
     * @param token The verification token string.
     * @return A map indicating success or failure.
     */
    Map<String, Object> verifyToken(String token);

}