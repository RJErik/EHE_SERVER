package com.example.ehe_server.exception.custom;

public class ApiKeyNotFoundOrUnauthorizedException extends ResourceNotFoundException {
    public ApiKeyNotFoundOrUnauthorizedException() {
        super("error.message.API_KEY_NOT_FOUND", "error.logDetail.API_KEY_NOT_FOUND");
    }
}
