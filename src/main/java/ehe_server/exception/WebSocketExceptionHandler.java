package ehe_server.exception;

import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.CustomBaseException;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class WebSocketExceptionHandler {

    private static final String DEFAULT_ERROR_MESSAGE_KEY = "error.message.default";
    private static final String DEFAULT_LOG_DETAIL_KEY = "error.logDetail.default";
    private static final String SIMP_DESTINATION_HEADER = "simpDestination";
    private static final String DEFAULT_WEBSOCKET_QUEUE = "/queue/errors";

    private final LoggingServiceInterface loggingService;
    private final MessageSource messageSource;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketExceptionHandler(
            LoggingServiceInterface loggingService,
            MessageSource messageSource,
            SimpMessagingTemplate messagingTemplate) {
        this.loggingService = loggingService;
        this.messageSource = messageSource;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageExceptionHandler(CustomBaseException.class)
    public void handleCustomBaseException(CustomBaseException ex, Message<?> message) {
        String defaultLogMessage = messageSource.getMessage(DEFAULT_LOG_DETAIL_KEY, null, "Default log message", LocaleContextHolder.getLocale());
        String defaultUserMessage = messageSource.getMessage(DEFAULT_ERROR_MESSAGE_KEY, null, "An error occurred.", LocaleContextHolder.getLocale());

        String destination = extractDestination(message);
        String prefix = getContextPrefix(destination);

        String logPattern = messageSource.getMessage(ex.getLogDetailKey(), null, defaultLogMessage, LocaleContextHolder.getLocale());
        String logMessage = MessageFormat.format(logPattern, ex.getLogArgs());
        String finalLogMessage = prefix + " " + logMessage;
        loggingService.logError(finalLogMessage, ex);

        String baseUserMessage = messageSource.getMessage(ex.getMessage(), null, defaultUserMessage, LocaleContextHolder.getLocale());
        String finalUserMessage = prefix + " " + baseUserMessage;

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", finalUserMessage);

        if (ex.getActionLink() != null) {
            errorResponse.put("actionLink", ex.getActionLink());
        }
        if (ex.isShowResendButton()) {
            errorResponse.put("showResendButton", true);
        }

        sendErrorToUser(message, destination, errorResponse);
    }

    @MessageExceptionHandler(Exception.class)
    public void handleGenericException(Exception ex, Message<?> message) {
        loggingService.logError("Unhandled WebSocket exception: " + ex.getMessage(), ex);

        String destination = extractDestination(message);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred. Please contact support.");

        sendErrorToUser(message, destination, errorResponse);
    }

    /**
     * Resolves the error context prefix with fallback chain:
     * 1. Try DESTINATION (e.g., /queue/automated-trades)
     * 2. Fall back to default
     */
    private String getContextPrefix(String destination) {
        String destinationKey = "error.context." + destination;
        String prefix = messageSource.getMessage(destinationKey, null, null, LocaleContextHolder.getLocale());

        if (prefix == null) {
            prefix = messageSource.getMessage("error.context.default", null, "An error occurred:", LocaleContextHolder.getLocale());
        }

        return prefix;
    }

    /**
     * Extracts the destination from the WebSocket message headers.
     * Falls back to a default error queue if destination cannot be determined.
     */
    private String extractDestination(Message<?> message) {
        if (message == null || message.getHeaders() == null) {
            return DEFAULT_WEBSOCKET_QUEUE;
        }

        String destination = message.getHeaders().get(SIMP_DESTINATION_HEADER, String.class);
        return destination != null ? destination : DEFAULT_WEBSOCKET_QUEUE;
    }

    /**
     * Sends the error response back to the user on the original destination.
     */
    private void sendErrorToUser(Message<?> message, String destination, Map<String, Object> errorResponse) {
        try {
            messagingTemplate.convertAndSend(destination, errorResponse);
        } catch (Exception e) {
            loggingService.logError("Failed to send WebSocket error response to " + destination, e);
        }
    }
}