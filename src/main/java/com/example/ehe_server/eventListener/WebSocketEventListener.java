package com.example.ehe_server.eventListener;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final LoggingServiceInterface loggingService;
    private final WebSocketSessionRegistry sessionRegistry;

    public WebSocketEventListener(
            LoggingServiceInterface loggingService,
            WebSocketSessionRegistry sessionRegistry) {
        this.loggingService = loggingService;
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (headerAccessor.getUser() instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof String) {
                System.out.println("WebSocket connected - Session ID: " + sessionId);
                loggingService.logAction("WebSocket connected - Session ID: " + sessionId);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (headerAccessor.getUser() instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof String) {
                System.out.println("WebSocket disconnected - Session ID: " + sessionId);
                loggingService.logAction("WebSocket disconnected - Session ID: " + sessionId);

                // Clean up all subscriptions for this session
                sessionRegistry.cleanupSession(sessionId);
            }
        }
    }
}