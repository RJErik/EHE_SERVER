package com.example.ehe_server.exception.custom;

public class MissingStartSequenceNumberException extends ValidationException {
    public MissingStartSequenceNumberException() {
        super("error.message.missingStartSequenceNumber", "error.logDetail.missingStartSequenceNumber");
    }
}