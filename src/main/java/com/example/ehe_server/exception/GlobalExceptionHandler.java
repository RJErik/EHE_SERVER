package com.example.ehe_server.exception;

import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public GlobalExceptionHandler(
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        // Try to get current user from audit context
        String contextUserId;
        Integer userId = null;

        try {
            contextUserId = auditContextService.getCurrentUser();
            // Try to parse the userId if it's not "unauthenticated" or "unknown"
            if (contextUserId != null && !contextUserId.equals("unknown")) {
                try {
                    userId = Integer.parseInt(contextUserId);
                } catch (NumberFormatException e) {
                    // If parsing fails, keep userId as null
                }
            }
        } catch (Exception e) {
            // If there's an error getting the current user, default to "unknown"
            contextUserId = "unknown";
        }

        // Log the error with appropriate user context
        loggingService.logError(userId, contextUserId, "Unhandled exception: " + ex.getMessage(), ex);

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
