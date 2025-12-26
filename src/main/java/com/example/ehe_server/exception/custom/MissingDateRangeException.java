package com.example.ehe_server.exception.custom;

public class MissingDateRangeException extends ValidationException {
    public MissingDateRangeException() {
        super("error.message.missingDateRange", "error.logDetail.missingDateRange");
    }
}