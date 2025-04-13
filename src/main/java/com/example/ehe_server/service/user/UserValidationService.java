package com.example.ehe_server.service.user;

import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserValidationServiceInterface;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserValidationService implements UserValidationServiceInterface {

    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public UserValidationService(
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> verifyUser() {
        // Current user context should be set by the controller
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);

//        System.out.println("VERIFY SERVICE: Current user = " + auditContextService.getCurrentUser());
//        System.out.println("VERIFY SERVICE: Current role = " + auditContextService.getCurrentUserRole());

        // Log the action
        loggingService.logAction(
                Integer.parseInt(auditContextService.getCurrentUser()),
                auditContextService.getCurrentUser(),
                "User verification successful."
        );

        return responseBody;
    }
}
