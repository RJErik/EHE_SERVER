package com.example.ehe_server.exception.custom;

public class RequiredFieldsMissingException extends ValidationException {
    public RequiredFieldsMissingException() {
        super("error.message.REQUIRED_FIELDS_MISSING", "error.logDetail.REQUIRED_FIELDS_MISSING");
    }
}
