package com.example.ehe_server.exception.custom;

public class MissingAlertIdException extends ValidationException {
    public MissingAlertIdException() {
        super("error.message.missingAlertId", "error.logDetail.missingAlertId");
    }
}