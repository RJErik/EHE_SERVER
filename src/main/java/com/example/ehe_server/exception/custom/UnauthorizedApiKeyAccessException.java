package com.example.ehe_server.exception.custom;

public class UnauthorizedApiKeyAccessException extends ResourceNotFoundException {
    public UnauthorizedApiKeyAccessException(Integer userId, Integer apiKeyId) {
        super(
                "error.message.apiKeyNotFound",
                "error.logDetail.unauthorizedApiKeyAccess",
                userId,
                apiKeyId
        );
    }
}