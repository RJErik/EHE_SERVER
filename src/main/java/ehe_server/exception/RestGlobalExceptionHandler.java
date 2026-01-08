package ehe_server.exception;

import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.*;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.annotation.validation.ExceptionInstantiator;
import ehe_server.annotation.validation.ValidationMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.HandlerMapping;

import java.text.MessageFormat;
import java.util.*;

@ControllerAdvice
public class RestGlobalExceptionHandler {

    private static final String DEFAULT_ERROR_MESSAGE_KEY = "error.message.default";
    private static final String DEFAULT_LOG_DETAIL_KEY = "error.logDetail.default";
    private static final String VALUE_PLACEHOLDER = "$value";

    private final LoggingServiceInterface loggingService;
    private final MessageSource messageSource;
    private final ExceptionInstantiator exceptionInstantiator;

    public RestGlobalExceptionHandler(
            LoggingServiceInterface loggingService,
            MessageSource messageSource,
            ExceptionInstantiator exceptionInstantiator) {
        this.loggingService = loggingService;
        this.messageSource = messageSource;
        this.exceptionInstantiator = exceptionInstantiator;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String method = request.getMethod();
        String bestMatchPattern = (String) request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String prefix = getContextPrefix(method, bestMatchPattern);

        Locale locale = LocaleContextHolder.getLocale();

        List<String> userMessages = new ArrayList<>();
        List<String> logMessages = new ArrayList<>();
        Map<String, String> actionLink = null;
        boolean showResendButton = false;

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String rawMessage = fieldError.getDefaultMessage();
            Object rejectedValue = fieldError.getRejectedValue();

            if (ValidationMetadata.isValidJson(rawMessage)) {
                ValidationMetadata metadata = ValidationMetadata.fromJson(rawMessage);

                if (metadata != null) {
                    processValidationMetadata(
                            metadata,
                            rejectedValue,
                            locale,
                            userMessages,
                            logMessages
                    );

                    if (actionLink == null && metadata.hasActionLink()) {
                        actionLink = Map.of(
                                "text", metadata.getActionLinkText(),
                                "target", metadata.getActionLinkTarget()
                        );
                    }

                    if (metadata.isShowResendButton()) {
                        showResendButton = true;
                    }

                    continue;
                }
            }

            userMessages.add(rawMessage);
            logMessages.add(rawMessage);
        }

        String aggregatedUserMessage = prefix + " " + String.join(" ", userMessages);
        String aggregatedLogMessage = prefix + " " + String.join(" ", logMessages);

        loggingService.logAction(aggregatedLogMessage);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", aggregatedUserMessage);

        if (actionLink != null) {
            errorResponse.put("actionLink", actionLink);
        }
        if (showResendButton) {
            errorResponse.put("showResendButton", true);
        }

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private void processValidationMetadata(
            ValidationMetadata metadata,
            Object rejectedValue,
            Locale locale,
            List<String> userMessages,
            List<String> logMessages) {

        Optional<CustomBaseException> exceptionOpt = exceptionInstantiator
                .instantiate(metadata.getExceptionClass());

        if (exceptionOpt.isEmpty()) {
            userMessages.add("Validation error occurred.");
            logMessages.add("Could not instantiate exception: " + metadata.getExceptionClass());
            return;
        }

        CustomBaseException exception = exceptionOpt.get();

        Object[] logParams = buildParams(metadata.getParams(), rejectedValue);

        String userMessage = resolveMessage(
                exception.getMessage(),
                new Object[]{},
                DEFAULT_ERROR_MESSAGE_KEY,
                locale
        );
        userMessages.add(userMessage);

        String logMessage = resolveMessage(
                exception.getLogDetailKey(),
                logParams,
                DEFAULT_LOG_DETAIL_KEY,
                locale
        );
        logMessages.add(logMessage);
    }

    private Object[] buildParams(String[] annotationParams, Object rejectedValue) {
        if (annotationParams == null || annotationParams.length == 0) {
            return new Object[]{};
        }

        List<Object> params = new ArrayList<>();

        for (String param : annotationParams) {
            if (VALUE_PLACEHOLDER.equals(param)) {
                params.add(rejectedValue != null ? rejectedValue : "null");
            } else {
                params.add(param);
            }
        }

        return params.toArray();
    }

    @ExceptionHandler(CustomBaseException.class)
    public ResponseEntity<Map<String, Object>> handleCustomBaseException(
            CustomBaseException ex,
            HttpServletRequest request) {

        HttpStatus status = determineHttpStatus(ex);
        Locale locale = LocaleContextHolder.getLocale();

        String method = request.getMethod();
        String bestMatchPattern = (String) request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String prefix = getContextPrefix(method, bestMatchPattern);

        String logMessage = resolveMessage(
                ex.getLogDetailKey(),
                ex.getLogArgs(),
                DEFAULT_LOG_DETAIL_KEY,
                locale
        );
        String finalLogMessage = prefix + " " + logMessage;
        loggingService.logAction(finalLogMessage);

        String userMessage = resolveMessage(
                ex.getMessage(),
                new Object[]{},
                DEFAULT_ERROR_MESSAGE_KEY,
                locale
        );
        String finalUserMessage = prefix + " " + userMessage;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        loggingService.logError("Unhandled exception: " + ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred. Please contact support.");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolveMessage(String key, Object[] args, String defaultKey, Locale locale) {
        String defaultMessage = messageSource.getMessage(
                defaultKey, null, "An error occurred.", locale);

        String pattern = messageSource.getMessage(key, null, defaultMessage, locale);

        if (args != null && args.length > 0) {
            return MessageFormat.format(pattern, args);
        }
        return pattern;
    }

    private String getContextPrefix(String method, String pattern) {
        Locale locale = LocaleContextHolder.getLocale();

        String methodSpecificKey = "error.context." + method + "." + pattern;
        String prefix = messageSource.getMessage(methodSpecificKey, null, null, locale);

        if (prefix == null && pattern != null) {
            String patternOnlyKey = "error.context." + pattern;
            prefix = messageSource.getMessage(patternOnlyKey, null, null, locale);
        }

        if (prefix == null) {
            prefix = messageSource.getMessage(
                    "error.context.default", null, "An error occurred:", locale);
        }

        return prefix;
    }

    private HttpStatus determineHttpStatus(CustomBaseException ex) {
        if (ex instanceof ValidationException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof AuthorizationException) return HttpStatus.FORBIDDEN;
        if (ex instanceof ResourceNotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof BusinessRuleException) return HttpStatus.CONFLICT;
        if (ex instanceof ExternalServiceException) return HttpStatus.SERVICE_UNAVAILABLE;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}