package com.example.ehe_server.exception.custom;

public class MissingPageNumberException extends ValidationException {
    public MissingPageNumberException() {
        super("error.message.missingPageNumber", "error.logDetail.missingPageNumber");
    }
}