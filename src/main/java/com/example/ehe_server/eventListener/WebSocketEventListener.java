package com.example.ehe_server.eventListener;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final LoggingServiceInterface loggingService;

    public WebSocketEventListener(LoggingServiceInterface loggingService) {
        this.loggingService = loggingService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() instanceof Authentication) {
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth.getPrincipal() instanceof Long) {
                Long userId = (Long) auth.getPrincipal();
                loggingService.logAction(userId.intValue(), userId.toString(), "WebSocket connected");
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() instanceof Authentication) {
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth.getPrincipal() instanceof Long) {
                Long userId = (Long) auth.getPrincipal();
                loggingService.logAction(userId.intValue(), userId.toString(), "WebSocket disconnected");
            }
        }
    }
}
