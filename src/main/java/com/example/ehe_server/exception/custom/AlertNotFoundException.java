package com.example.ehe_server.exception.custom;

public class AlertNotFoundException extends ResourceNotFoundException {
    public AlertNotFoundException(Integer alertId) {
        super("error.message.alertNotFound", "error.logDetail.alertNotFound", alertId);
    }
}