package com.example.ehe_server.service.user;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserValidationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserValidationService implements UserValidationServiceInterface {

    private final LoggingServiceInterface loggingService;

    public UserValidationService(
            LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public void verifyUser() {
        // Log the action
        loggingService.logAction("User verification successful.");
    }
}
