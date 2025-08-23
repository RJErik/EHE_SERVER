package com.example.ehe_server.service.audit;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service to handle WebSocket authentication operations
 */
@Service
public class WebSocketAuthService implements WebSocketAuthServiceInterface {

    private final LoggingServiceInterface loggingService;

    public WebSocketAuthService(LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    public Integer getUserIdFromWebSocketAuth(StompHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            loggingService.logError("StompHeaderAccessor is null", null);
            return null;
        }

        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth == null) {
            loggingService.logError("No authentication found in WebSocket header", null);
            return null;
        }

        Object principal = auth.getPrincipal();

        // Handle different principal types
        if (principal instanceof String) {
            try {
                return Integer.parseInt((String) principal);
            } catch (NumberFormatException e) {
                loggingService.logError("Failed to parse user ID from string principal: " + principal, e);
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

    @Override
    public boolean isWebSocketUserAuthenticated(StompHeaderAccessor headerAccessor) {
        return getUserIdFromWebSocketAuth(headerAccessor) != null;
    }
}