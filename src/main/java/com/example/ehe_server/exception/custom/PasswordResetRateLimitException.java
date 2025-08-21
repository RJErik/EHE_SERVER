package com.example.ehe_server.exception.custom;

public class PasswordResetRateLimitException extends BusinessRuleException {
    public PasswordResetRateLimitException(String email, int maxRequests, int minutes) {
        super("error.message.passwordResetRateLimit", "error.logDetail.passwordResetRateLimit", email, maxRequests, minutes);
    }
}