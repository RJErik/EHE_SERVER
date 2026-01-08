package ehe_server.exception.custom;

public class EmailChangeRateLimitExceededException extends BusinessRuleException {
    public EmailChangeRateLimitExceededException(int minutes) {
        super("error.message.emailChangeRateLimit", "error.logDetail.emailChangeRateLimit", minutes);
    }
}
