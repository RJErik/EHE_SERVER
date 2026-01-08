package ehe_server.exception.custom;

public class ApiKeyMissingException extends BusinessRuleException {
    public ApiKeyMissingException(Integer portfolioId) {
        super("error.message.apiKeyMissing", "error.logDetail.apiKeyMissing", portfolioId);
    }
}