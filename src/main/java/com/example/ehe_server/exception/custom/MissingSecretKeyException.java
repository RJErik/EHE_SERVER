package com.example.ehe_server.exception.custom;

public class MissingSecretKeyException extends ValidationException {
    public MissingSecretKeyException() {
        super("error.message.missingSecretKey", "error.logDetail.missingSecretKey");
    }
}