package ehe_server.exception.custom;

/**
 * This file contains the definitions for the standardized exception categories
 * and examples of how they would be used in a service.
 *
 * All these exceptions extend the CustomBaseException to ensure they can be
 * handled by the RestGlobalExceptionHandler and carry the necessary data for
 * logging and building the API response.
 */


// ----------------------------------------------------------------
// 1. ValidationException (for HTTP 400 Bad Request)
// ----------------------------------------------------------------

/**
 * Thrown when user input fails validation (e.g., missing fields, invalid format,
 * password too weak).
 */
public class ValidationException extends CustomBaseException {
    public ValidationException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey, logDetailKey, logArgs);
    }
}
