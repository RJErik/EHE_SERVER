package com.example.ehe_server.exception.custom;

import com.example.ehe_server.exception.custom.AuthorizationException;

public class UnauthorizedAlertAccessException extends AuthorizationException {
    public UnauthorizedAlertAccessException(Integer userId, Integer alertId) {
        super("error.message.unauthorizedAlertAccess", "error.logDetail.unauthorizedAlertAccess", userId, alertId);
    }
}