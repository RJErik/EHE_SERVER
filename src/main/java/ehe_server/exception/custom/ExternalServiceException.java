package ehe_server.exception.custom;

// ----------------------------------------------------------------
// 5. ExternalServiceException (for HTTP 503 Service Unavailable)
// ----------------------------------------------------------------

/**
 * Thrown when a required external service (e.g., a third-party API like
 * Binance, or an email sending service) fails or is unavailable.
 */
public class ExternalServiceException extends CustomBaseException {
    public ExternalServiceException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey, logDetailKey, logArgs);
    }
}
