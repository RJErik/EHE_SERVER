package com.example.ehe_server.exception.custom;

// ----------------------------------------------------------------
// 4. BusinessRuleException (for HTTP 409 Conflict)
// ----------------------------------------------------------------

/**
 * Thrown when a request is valid but violates a business rule (e.g., email
 * already exists, token is expired, item already in watchlist).
 */
public class BusinessRuleException extends CustomBaseException {
    public BusinessRuleException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey, logDetailKey, logArgs);
    }
}
