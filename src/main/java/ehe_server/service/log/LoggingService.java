package ehe_server.service.log;

import ehe_server.entity.ErrorLog;
import ehe_server.entity.Log;
import ehe_server.entity.User;
import ehe_server.repository.ErrorLogRepository;
import ehe_server.repository.LogRepository;
import ehe_server.repository.UserRepository;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.log.LoggingServiceInterface;
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
        Log log = new Log();
        log.setLogDescription(action);

        // Set user if it's a human user
        if (userContextService.isHumanUser()) {
            Optional<User> user = userRepository.findById(userContextService.getCurrentUserId());
            user.ifPresent(log::setUser);
        }

        logRepository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String errorDescription, Throwable throwable) {
        // Convert throwable to stack trace string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorDescription(errorDescription);
        errorLog.setStackTrace(stackTrace != null ? stackTrace : "No stack trace available");

        // Set user if it's a human user
        if (userContextService.isHumanUser()) {
            Optional<User> user = userRepository.findById(userContextService.getCurrentUserId());
            user.ifPresent(errorLog::setUser);
        }

        errorLogRepository.save(errorLog);
    }
}
