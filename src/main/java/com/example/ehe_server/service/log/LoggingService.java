package com.example.ehe_server.service.log;

import com.example.ehe_server.entity.ErrorLog;
import com.example.ehe_server.entity.Log;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ErrorLogRepository;
import com.example.ehe_server.repository.LogRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class LoggingService implements LoggingServiceInterface {

    private final LogRepository logRepository;
    private final ErrorLogRepository errorLogRepository;
    private final UserRepository userRepository;
    private final AuditContextService auditContextService;

    public LoggingService(
            LogRepository logRepository,
            ErrorLogRepository errorLogRepository,
            UserRepository userRepository,
            AuditContextService auditContextService) {
        this.logRepository = logRepository;
        this.errorLogRepository = errorLogRepository;
        this.userRepository = userRepository;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Integer userId, String contextUserId, String action) {
        // Set the user context for database operations
        auditContextService.setCurrentUser(contextUserId != null ? contextUserId : "unknown");

        // Get user for the log table (can't be null)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // Create log entry
        Log log = new Log();
        log.setUser(user);
        log.setLogDescription(action);
        logRepository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(Integer userId, String contextUserId, String errorDescription, Throwable throwable) {
        // Set the user context for database operations
        auditContextService.setCurrentUser(contextUserId != null ? contextUserId : "unknown");

        // Get user for the log table (can't be null)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // Convert throwable to stack trace string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Create error log entry
        ErrorLog errorLog = new ErrorLog();
        errorLog.setUser(user);
        errorLog.setErrorDescription(errorDescription);
        errorLog.setStackTrace(stackTrace != null ? stackTrace : "No stack trace available");
        errorLogRepository.save(errorLog);
    }
}
