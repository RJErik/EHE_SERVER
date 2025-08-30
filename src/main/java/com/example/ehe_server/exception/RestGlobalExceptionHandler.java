package com.example.ehe_server.exception;

import com.example.ehe_server.securityConfig.ApiErrorContextProperties;
import com.example.ehe_server.exception.custom.*; // Assuming new exceptions are in this package
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.HandlerMapping;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestGlobalExceptionHandler {
    // Keys for default messages from messages.properties
    private static final String DEFAULT_ERROR_MESSAGE_KEY = "error.message.default";
    private static final String DEFAULT_LOG_DETAIL_KEY = "error.logDetail.default";

    private final LoggingServiceInterface loggingService;
    private final MessageSource messageSource;
    private final ApiErrorContextProperties errorContextProperties;

    public RestGlobalExceptionHandler(
            LoggingServiceInterface loggingService,
            MessageSource messageSource,
            ApiErrorContextProperties errorContextProperties) {
        this.loggingService = loggingService;
        this.messageSource = messageSource;
        this.errorContextProperties = errorContextProperties;
    }

    // Catch all custom exceptions and delegate to a common builder
    @ExceptionHandler(CustomBaseException.class)
    public ResponseEntity<Map<String, Object>> handleCustomBaseException(CustomBaseException ex, HttpServletRequest request) {
        HttpStatus status = determineHttpStatus(ex);

        // Resolve default messages from messages.properties to use as fallbacks
        String defaultLogMessage = messageSource.getMessage(DEFAULT_LOG_DETAIL_KEY, null, "Default log message", LocaleContextHolder.getLocale());
        String defaultUserMessage = messageSource.getMessage(DEFAULT_ERROR_MESSAGE_KEY, null, "An error occurred.", LocaleContextHolder.getLocale());

        // 1. Get the URL-based prefix first, so it can be used for both logging and the user message.
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String prefix = errorContextProperties.getMapping().getOrDefault(bestMatchPattern, errorContextProperties.getDefaultContext());

        // 2. Resolve, format, and log the detailed error message WITH the prefix.
        String logPattern = messageSource.getMessage(ex.getLogDetailKey(), null, defaultLogMessage, LocaleContextHolder.getLocale());
        String logMessage = MessageFormat.format(logPattern, ex.getLogArgs());
        String finalLogMessage = prefix + " " + logMessage; // Combine prefix and log message
        loggingService.logError(finalLogMessage, ex); // Log the combined message

        // 3. Resolve and format the user-facing message WITH the prefix.
        String baseUserMessage = messageSource.getMessage(ex.getMessage(), null, defaultUserMessage, LocaleContextHolder.getLocale());
        String finalUserMessage = prefix + " " + baseUserMessage;

        // 4. Build the response body from the exception's data
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", finalUserMessage);

        // Add actionLink and showResendButton if they exist in the exception
        if (ex.getActionLink() != null) {
            errorResponse.put("actionLink", ex.getActionLink());
        }
        if (ex.isShowResendButton()) {
            errorResponse.put("showResendButton", true);
        }

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Determines the appropriate HTTP status code based on the exception's category.
     */
    private HttpStatus determineHttpStatus(CustomBaseException ex) {
        if (ex instanceof ValidationException) return HttpStatus.BAD_REQUEST; // 400
        if (ex instanceof AuthorizationException) return HttpStatus.FORBIDDEN; // 403
        if (ex instanceof ResourceNotFoundException) return HttpStatus.NOT_FOUND; // 404
        if (ex instanceof BusinessRuleException) return HttpStatus.CONFLICT; // 409
        if (ex instanceof ExternalServiceException) return HttpStatus.SERVICE_UNAVAILABLE; // 503
        return HttpStatus.INTERNAL_SERVER_ERROR; // Default fallback
    }

    /**
     * Handles all other unexpected exceptions as a final catch-all.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        loggingService.logError("Unhandled exception: " + ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred. Please contact support.");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
