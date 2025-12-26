package com.example.ehe_server.exception.custom;

public class MissingSequenceNumberException extends ValidationException {
    public MissingSequenceNumberException() {
        super("error.message.missingSequenceNumber", "error.logDetail.missingSequenceNumber");
    }
}