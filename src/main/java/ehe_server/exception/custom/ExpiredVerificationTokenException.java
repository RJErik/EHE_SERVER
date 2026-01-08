package ehe_server.exception.custom;

public class ExpiredVerificationTokenException extends BusinessRuleException {
    public ExpiredVerificationTokenException(String token) {
        super("error.message.expiredVerificationToken", "error.logDetail.expiredVerificationToken", token);
    }
}