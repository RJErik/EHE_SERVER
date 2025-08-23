package com.example.ehe_server.service.intf.audit;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * Interface for WebSocket authentication services
 */
public interface WebSocketAuthServiceInterface {

    /**
     * Extract user ID from WebSocket authentication
     *
     * @param headerAccessor The STOMP header accessor containing authentication info
     * @return User ID as Integer, null if not authenticated or invalid
     */
    Integer getUserIdFromWebSocketAuth(StompHeaderAccessor headerAccessor);

    /**
     * Check if the WebSocket user is authenticated
     *
     * @param headerAccessor The STOMP header accessor containing authentication info
     * @return true if authenticated, false otherwise
     */
    boolean isWebSocketUserAuthenticated(StompHeaderAccessor headerAccessor);
}
