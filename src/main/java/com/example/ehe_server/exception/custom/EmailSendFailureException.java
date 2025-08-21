package com.example.ehe_server.exception.custom;

public class EmailSendFailureException extends ExternalServiceException {
    public EmailSendFailureException(String recipient, String detail) {
        super("error.message.emailSendFailure", "error.logDetail.emailSendFailure", recipient, detail);
    }
}