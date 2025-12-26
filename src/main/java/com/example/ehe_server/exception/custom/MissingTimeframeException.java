package com.example.ehe_server.exception.custom;

public class MissingTimeframeException extends ValidationException {
    public MissingTimeframeException() {
        super("error.message.missingTimeframe", "error.logDetail.missingTimeframe");
    }
}