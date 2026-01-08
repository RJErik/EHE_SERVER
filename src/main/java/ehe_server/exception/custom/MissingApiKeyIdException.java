package ehe_server.exception.custom;

public class MissingApiKeyIdException extends ValidationException {
    public MissingApiKeyIdException() {
        super("error.message.missingApiKeyId", "error.logDetail.missingApiKeyId");
    }
}