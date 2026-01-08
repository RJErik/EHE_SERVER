package ehe_server.exception.custom;

// ----------------------------------------------------------------
// 3. AuthorizationException (for HTTP 403 Forbidden)
// ----------------------------------------------------------------

/**
 * Thrown when a user is authenticated but not authorized to perform an action
 * or access a resource (e.g., accessing another user's data).
 */
public class AuthorizationException extends CustomBaseException {
    public AuthorizationException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey, logDetailKey, logArgs);
    }
}
