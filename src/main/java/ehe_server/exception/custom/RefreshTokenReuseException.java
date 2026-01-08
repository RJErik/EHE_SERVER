package ehe_server.exception.custom;

/**
 * Thrown when a refresh token is valid (signature-wise) but not found in the database,
 * indicating potential token theft and reuse.
 */
public class RefreshTokenReuseException extends AuthorizationException {
  public RefreshTokenReuseException() {
    super(
            "error.message.refreshTokenNotFound",
            "error.logDetail.refreshTokenReuse"
    );
  }
}