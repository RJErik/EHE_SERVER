package com.example.ehe_server.service.user;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserValidationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserValidationService implements UserValidationServiceInterface {

    private final LoggingServiceInterface loggingService;

    public UserValidationService(
            LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public Map<String, Object> verifyUser() {
        // Current user context should be set by the controller
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);

//        System.out.println("VERIFY SERVICE: Current user = " + auditContextService.getCurrentUser());
//        System.out.println("VERIFY SERVICE: Current role = " + auditContextService.getCurrentUserRole());

        // Log the action
        loggingService.logAction("User verification successful."
        );

        return responseBody;
    }
}
