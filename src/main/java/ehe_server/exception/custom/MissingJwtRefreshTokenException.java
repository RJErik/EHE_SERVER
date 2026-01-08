package ehe_server.exception.custom;

public class MissingJwtRefreshTokenException extends ValidationException {
    public MissingJwtRefreshTokenException() {
        super("error.message.missingJwtRefreshToken", "error.logDetail.missingJwtRefreshToken");
    }
}