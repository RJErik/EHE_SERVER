package com.example.ehe_server.exception;

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

    public GlobalExceptionHandler(
            LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        // Log the error with appropriate user context
        loggingService.logError("Unhandled exception: " + ex.getMessage(), ex);

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
