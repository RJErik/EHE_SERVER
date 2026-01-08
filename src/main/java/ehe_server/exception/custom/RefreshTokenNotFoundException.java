package ehe_server.exception.custom;

/**
 * Thrown when no refresh token cookie is present in the request.
 */
public class RefreshTokenNotFoundException extends AuthorizationException {
    public RefreshTokenNotFoundException() {
        super(
                "error.message.refreshTokenNotFound",
                "error.logDetail.refreshTokenNotFound"
        );
    }
}