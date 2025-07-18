package com.example.ehe_server.service.log;

import com.example.ehe_server.entity.ErrorLog;
import com.example.ehe_server.entity.Log;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ErrorLogRepository;
import com.example.ehe_server.repository.LogRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Service
@Transactional
public class LoggingService implements LoggingServiceInterface {

    private final LogRepository logRepository;
    private final ErrorLogRepository errorLogRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    public LoggingService(
            LogRepository logRepository,
            ErrorLogRepository errorLogRepository,
            UserRepository userRepository,
            UserContextService userContextService) {
        this.logRepository = logRepository;
        this.errorLogRepository = errorLogRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action) {
        // Create log entry
        if (!userContextService.isAuthenticated() || userContextService.isHumanUser()) {
            Log log = new Log();
            log.setLogDescription(action);
            logRepository.save(log);
        } else if (userContextService.isHumanUser()) {
            Optional<User> user = userRepository.findById(userContextService.getCurrentUserId().intValue());
            Log log = new Log();
            if (user.isPresent()) {
                log.setUser(user.get());
            }
            log.setLogDescription(action);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String errorDescription, Throwable throwable) {

        // Convert throwable to stack trace string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Create error log entry
        if (!userContextService.isAuthenticated() || userContextService.isHumanUser()) {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setErrorDescription(errorDescription);
            errorLog.setStackTrace(stackTrace != null ? stackTrace : "No stack trace available");
            errorLogRepository.save(errorLog);
        } else if (userContextService.isHumanUser()) {
            Optional<User> user = userRepository.findById(userContextService.getCurrentUserId().intValue());
            ErrorLog errorLog = new ErrorLog();
            if (user.isPresent()) {
                errorLog.setUser(user.get());
            }
            errorLog.setErrorDescription(errorDescription);
            errorLog.setStackTrace(stackTrace != null ? stackTrace : "No stack trace available");
            errorLogRepository.save(errorLog);
        }
    }
}
