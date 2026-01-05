package com.example.ehe_server.aspect;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private final MessageSource messageSource;
    private final LoggingServiceInterface loggingService;

    public LoggingAspect(MessageSource messageSource,
                         LoggingServiceInterface loggingService) {
        this.messageSource = messageSource;
        this.loggingService = loggingService;
    }

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(logMessage)")
    public Object logAround(ProceedingJoinPoint joinPoint, LogMessage logMessage) throws Throwable {

        Object result = joinPoint.proceed();

        logMessage(joinPoint, logMessage, result);

        return result;
    }

    private void logMessage(ProceedingJoinPoint joinPoint, LogMessage logMessage, Object result) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add method parameters to context
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] paramValues = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], paramValues[i]);
            }

            if (result != null) {
                context.setVariable("result", result);
            }

            Object[] messageParams = new Object[logMessage.params().length];
            for (int i = 0; i < logMessage.params().length; i++) {
                Expression expression = parser.parseExpression(logMessage.params()[i]);
                messageParams[i] = expression.getValue(context);
            }

            String message = messageSource.getMessage(
                    logMessage.messageKey(),
                    messageParams,
                    LocaleContextHolder.getLocale()
            );

            loggingService.logAction(message);

        } catch (Exception e) {
            loggingService.logError("Error processing log message", e);
        }
    }
}