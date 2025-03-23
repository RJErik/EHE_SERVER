package com.example.ehe_server.service.intf.log;

public interface LoggingServiceInterface {
    void logAction(Integer userId, String contextUserId, String action);
    void logError(Integer userId, String contextUserId, String errorDescription, Throwable throwable);
}
