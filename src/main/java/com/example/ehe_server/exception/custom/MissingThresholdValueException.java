package com.example.ehe_server.exception.custom;

public class MissingThresholdValueException extends ValidationException {
    public MissingThresholdValueException() {
        super("error.message.missingThresholdValue", "error.logDetail.missingThresholdValue");
    }
}