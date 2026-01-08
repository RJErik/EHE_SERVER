package ehe_server.service.intf.log;

public interface LoggingServiceInterface {
    void logAction(String action);
    void logError(String errorDescription, Throwable throwable);
}
