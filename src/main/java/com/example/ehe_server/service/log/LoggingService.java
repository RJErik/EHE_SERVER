package com.example.ehe_server.service.log;

import com.example.ehe_server.entity.ErrorLog;
import com.example.ehe_server.entity.Log;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ErrorLogRepository;
import com.example.ehe_server.repository.LogRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Service
public class LoggingService implements LoggingServiceInterface {

    private final LogRepository logRepository;
    private final ErrorLogRepository errorLogRepository;
    private final UserRepository userRepository;

    public LoggingService(
            LogRepository logRepository,
            ErrorLogRepository errorLogRepository,
            UserRepository userRepository) {
        this.logRepository = logRepository;
        this.errorLogRepository = errorLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action) {
        Log log = new Log();
        log.setUser(user);
        log.setLogDescription(action);
        logRepository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action) {
        User user = getCurrentUser();
        logAction(user, action);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(User user, String errorDescription, Throwable throwable) {
        // Convert throwable to stack trace string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        ErrorLog errorLog = new ErrorLog();
        errorLog.setUser(user);
        errorLog.setErrorDescription(errorDescription);
        errorLog.setStackTrace(stackTrace != null ? stackTrace : "No stack trace available");
        errorLogRepository.save(errorLog);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String errorDescription, Throwable throwable) {
        User user = getCurrentUser();
        logError(user, errorDescription, throwable);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long) {
                Integer userId = ((Long) principal).intValue();
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
        }
        return null;
    }
}