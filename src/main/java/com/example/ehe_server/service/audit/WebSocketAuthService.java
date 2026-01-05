package com.example.ehe_server.service.audit;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle WebSocket authentication operations
 */
@Service
@Transactional
public class WebSocketAuthService implements WebSocketAuthServiceInterface {

    private final LoggingServiceInterface loggingService;

    public WebSocketAuthService(LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public Integer getUserIdFromWebSocketAuth(StompHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }

        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof String) {
            try {
                return Integer.parseInt((String) principal);
            } catch (NumberFormatException e) {
                loggingService.logAction("Failed to parse user ID from string principal: " + principal);
                return null;
            }
        } else if (principal instanceof Long) {
            return ((Long) principal).intValue();
        } else if (principal instanceof Integer) {
            return (Integer) principal;
        }

        loggingService.logError("Unexpected principal type: " + (principal != null ? principal.getClass().getSimpleName() : "null"), null);
        return null;
    }
}