package com.example.ehe_server.exception.custom;

public class MissingTokenException extends ValidationException {
    public MissingTokenException() {
        super("error.message.missingToken", "error.logDetail.missingToken");
    }
}