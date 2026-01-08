package ehe_server.exception.custom;

public class NonVerifiedAccountException extends AuthorizationException {
    public NonVerifiedAccountException(String email, String status) {
        super("error.message.nonVerifiedAccount", "error.logDetail.nonVerifiedAccount", email, status);
    }
}