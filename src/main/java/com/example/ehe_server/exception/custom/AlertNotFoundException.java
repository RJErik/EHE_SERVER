package com.example.ehe_server.exception.custom;

import com.example.ehe_server.exception.custom.ResourceNotFoundException;

public class AlertNotFoundException extends ResourceNotFoundException {
    public AlertNotFoundException(Integer alertId) {
        super("error.message.alertNotFound", "error.logDetail.alertNotFound", alertId);
    }
}