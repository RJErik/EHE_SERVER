package ehe_server.exception.custom;

import java.time.LocalDateTime;

/**
 * Thrown when the session's anchor max expiry date has passed,
 * requiring the user to re-authenticate.
 */
public class SessionLimitExceededException extends AuthorizationException {
    public SessionLimitExceededException(LocalDateTime anchorExpiry) {
        super(
                "error.message.sessionLimitExceeded",
                "error.logDetail.sessionLimitExceeded",
                anchorExpiry.toString()
        );
    }
}