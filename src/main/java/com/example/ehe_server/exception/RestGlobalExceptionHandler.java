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

    public RestGlobalExceptionHandler(
            LoggingServiceInterface loggingService,
            MessageSource messageSource) {
        this.loggingService = loggingService;
        this.messageSource = messageSource;
    }

    // Catch all custom exceptions and delegate to a common builder
    @ExceptionHandler(CustomBaseException.class)
    public ResponseEntity<Map<String, Object>> handleCustomBaseException(CustomBaseException ex, HttpServletRequest request) {
        HttpStatus status = determineHttpStatus(ex);

        // Resolve default messages
        String defaultLogMessage = messageSource.getMessage(DEFAULT_LOG_DETAIL_KEY, null, "Default log message", LocaleContextHolder.getLocale());
        String defaultUserMessage = messageSource.getMessage(DEFAULT_ERROR_MESSAGE_KEY, null, "An error occurred.", LocaleContextHolder.getLocale());

        // 1. Get the HTTP method and URL pattern
        String method = request.getMethod(); // GET, POST, DELETE, etc.
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String prefix = getContextPrefix(method, bestMatchPattern);

        // 2. Resolve, format, and log the detailed error message WITH the prefix
        String logPattern = messageSource.getMessage(ex.getLogDetailKey(), null, defaultLogMessage, LocaleContextHolder.getLocale());
        String logMessage = MessageFormat.format(logPattern, ex.getLogArgs());
        String finalLogMessage = prefix + " " + logMessage;
        loggingService.logError(finalLogMessage, ex);

        // 3. Resolve and format the user-facing message WITH the prefix
        String baseUserMessage = messageSource.getMessage(ex.getMessage(), null, defaultUserMessage, LocaleContextHolder.getLocale());
        String finalUserMessage = prefix + " " + baseUserMessage;

        // 4. Build the response body
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", finalUserMessage);

        if (ex.getActionLink() != null) {
            errorResponse.put("actionLink", ex.getActionLink());
        }
        if (ex.isShowResendButton()) {
            errorResponse.put("showResendButton", true);
        }

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Resolves the error context prefix with fallback chain:
     * 1. Try METHOD.PATTERN (e.g., POST./api/user/alerts)
     * 2. Try just PATTERN (e.g., /api/user/alerts)
     * 3. Fall back to default
     */
    private String getContextPrefix(String method, String pattern) {
        // Try method-specific key first
        String methodSpecificKey = "error.context." + method + "." + pattern;
        String prefix = messageSource.getMessage(methodSpecificKey, null, null, LocaleContextHolder.getLocale());

        // If not found, try pattern-only key
        if (prefix == null) {
            String patternOnlyKey = "error.context." + pattern;
            prefix = messageSource.getMessage(patternOnlyKey, null, null, LocaleContextHolder.getLocale());
        }

        // If still not found, use default
        if (prefix == null) {
            prefix = messageSource.getMessage("error.context.default", null, "An error occurred:", LocaleContextHolder.getLocale());
        }

        return prefix;
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
