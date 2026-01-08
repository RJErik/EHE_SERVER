package ehe_server.exception.custom;

// ----------------------------------------------------------------
// 2. ResourceNotFoundException (for HTTP 404 Not Found)
// ----------------------------------------------------------------

/**
 * Thrown when a specific entity or resource cannot be found in the database.
 */
public class ResourceNotFoundException extends CustomBaseException {
    public ResourceNotFoundException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey, logDetailKey, logArgs);
    }
}