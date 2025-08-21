package com.example.ehe_server.exception.custom;

public class ApiKeyNotFoundException extends ResourceNotFoundException {
    public ApiKeyNotFoundException(Integer apiKeyId, Integer userId) {
        super("error.message.apiKeyNotFound", "error.logDetail.apiKeyNotFound", apiKeyId, userId);
    }
}