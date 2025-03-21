package com.example.ehe_server.service.intf.log;

import com.example.ehe_server.entity.User;

public interface LoggingServiceInterface {
    void logAction(User user, String action);
    void logAction(String action);
    void logError(User user, String errorDescription, Throwable throwable);
    void logError(String errorDescription, Throwable throwable);
}
