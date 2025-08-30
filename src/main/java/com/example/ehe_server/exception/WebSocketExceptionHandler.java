package com.example.ehe_server.exception;

import com.example.ehe_server.securityConfig.ApiErrorContextProperties;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class WebSocketExceptionHandler {

    // Keys for default messages from messages.properties
    private static final String DEFAULT_ERROR_MESSAGE_KEY = "error.message.default";
    private static final String DEFAULT_LOG_DETAIL_KEY = "error.logDetail.default";

    private final LoggingServiceInterface loggingService;
    private final MessageSource messageSource;
    private final ApiErrorContextProperties errorContextProperties;

    public WebSocketExceptionHandler(
            LoggingServiceInterface loggingService,
            MessageSource messageSource,
            ApiErrorContextProperties errorContextProperties) {
        this.loggingService = loggingService;
        this.messageSource = messageSource;
        this.errorContextProperties = errorContextProperties;
    }

    @MessageExceptionHandler(CustomBaseException.class)
    @SendToUser("/queue/candles")
    public Map<String, Object> handleCustomBaseException(CustomBaseException ex) {
        // Resolve default messages from messages.properties to use as fallbacks
        String defaultLogMessage = messageSource.getMessage(DEFAULT_LOG_DETAIL_KEY, null, "Default log message", LocaleContextHolder.getLocale());
        String defaultUserMessage = messageSource.getMessage(DEFAULT_ERROR_MESSAGE_KEY, null, "An error occurred.", LocaleContextHolder.getLocale());

        // For WebSocket, we'll use a generic WebSocket context prefix
        // You could modify this to be more specific based on the message destination
        String prefix = errorContextProperties.getMapping().getOrDefault("websocket", errorContextProperties.getDefaultContext());

        // Resolve, format, and log the detailed error message WITH the prefix
        String logPattern = messageSource.getMessage(ex.getLogDetailKey(), null, defaultLogMessage, LocaleContextHolder.getLocale());
        String logMessage = MessageFormat.format(logPattern, ex.getLogArgs());
        String finalLogMessage = prefix + " " + logMessage;
        loggingService.logError(finalLogMessage, ex);

        // Resolve and format the user-facing message WITH the prefix
        String baseUserMessage = messageSource.getMessage(ex.getMessage(), null, defaultUserMessage, LocaleContextHolder.getLocale());
        String finalUserMessage = prefix + " " + baseUserMessage;

        // Build the response body similar to REST controller
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

        return errorResponse;
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/candles")
    public Map<String, Object> handleGenericException(Exception ex) {
        loggingService.logError("Unhandled WebSocket exception: " + ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred. Please contact support.");

        return errorResponse;
    }
}