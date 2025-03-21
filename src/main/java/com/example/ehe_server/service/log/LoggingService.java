package com.example.ehe_server.service.log;

import com.example.ehe_server.service.context.ContextPropagationService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.entity.ErrorLog;
import com.example.ehe_server.entity.Log;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ErrorLogRepository;
import com.example.ehe_server.repository.LogRepository;
import com.example.ehe_server.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ContextPropagationService contextService;

    public LoggingService(
            LogRepository logRepository,
            ErrorLogRepository errorLogRepository,
            UserRepository userRepository,
            ContextPropagationService contextService) {
        this.logRepository = logRepository;
        this.errorLogRepository = errorLogRepository;
        this.userRepository = userRepository;
        this.contextService = contextService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, String action) {
        // Propagate context to this new transaction
        contextService.propagateCurrentContext();

        Log log = new Log();
        log.setUser(user);
        log.setLogDescription(action);
        logRepository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action) {
        // This will have JWT access via SecurityContextHolder
        User user = getCurrentUser();

        // Propagate context to this new transaction
        contextService.propagateCurrentContext();

        logAction(user, action);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(User user, String errorDescription, Throwable throwable) {
        // Propagate context to this new transaction
        contextService.propagateCurrentContext();

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

        // Propagate context to this new transaction
        contextService.propagateCurrentContext();

        logError(user, errorDescription, throwable);
    }

    // Helper method to get current user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof AnonymousAuthenticationToken)) {
            // Assuming the principal is the user ID
            String userId = auth.getPrincipal().toString();
            return userRepository.findById(Integer.valueOf(userId)).orElse(null);
        }
        return null;  // Or return a default system user
    }
}
