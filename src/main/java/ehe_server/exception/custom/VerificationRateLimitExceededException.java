package ehe_server.exception.custom;

public class VerificationRateLimitExceededException extends BusinessRuleException {
    public VerificationRateLimitExceededException(int minutes) {
        super("error.message.verificationRateLimit", "error.logDetail.verificationRateLimit", minutes);
    }
}