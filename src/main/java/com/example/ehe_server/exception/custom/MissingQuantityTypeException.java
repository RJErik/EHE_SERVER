package com.example.ehe_server.exception.custom;

public class MissingQuantityTypeException extends ValidationException {
    public MissingQuantityTypeException() {
        super("error.message.missingQuantityType", "error.logDetail.missingQuantityType");
    }
}