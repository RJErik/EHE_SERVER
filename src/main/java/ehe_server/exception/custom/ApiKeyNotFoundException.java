package ehe_server.exception.custom;

public class ApiKeyNotFoundException extends ResourceNotFoundException {
    public ApiKeyNotFoundException(Integer apiKeyId) {
        super("error.message.apiKeyNotFound", "error.logDetail.apiKeyNotFound", apiKeyId);
    }
}