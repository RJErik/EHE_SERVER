package ehe_server.exception.custom;

public class MissingApiKeyValueException extends ValidationException {
    public MissingApiKeyValueException() {
        super("error.message.missingApiKeyValue", "error.logDetail.missingApiKeyValue");
    }
}