package com.example.ehe_server.service.intf.audit;

import com.example.ehe_server.entity.User;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * Interface for managing user context throughout the application.
 * Supports both HTTP (SecurityContextHolder) and WebSocket (StompHeaderAccessor) authentication.
 */
public interface UserContextServiceInterface {

    // ==================== WebSocket Context Management ====================

    /**
     * Set the WebSocket header accessor for the current thread.
     * Call this at the start of WebSocket message handling.
     *
     * @param headerAccessor the STOMP header accessor from the WebSocket message
     */
    void setWebSocketContext(StompHeaderAccessor headerAccessor);

    /**
     * Clear the WebSocket context for the current thread.
     * Call this after WebSocket message handling is complete.
     */
    void clearWebSocketContext();

    // ==================== Authentication Methods ====================

    /**
     * Set the current user in SecurityContextHolder
     *
     * @param userId identifies who uses the code (be it users or automatic system job)
     * @param role the role of who uses the code
     */
    void setUser(String userId, String role);

    // ==================== User ID Methods ====================

    /**
     * Get the current authenticated user ID as Integer
     *
     * @return User ID if authenticated and numeric, null otherwise
     */
    Integer getCurrentUserId();

    /**
     * Get the current user's ID as a string for PostgreSQL audit context.
     * Supports multiple principal types from both HTTP and WebSocket contexts.
     *
     * @return User ID string, "UNKNOWN" if not authenticated
     */
    String getCurrentUserIdAsString();

    /**
     * Get the current user ID directly from a WebSocket header accessor.
     * Useful when you have direct access to the header accessor.
     *
     * @param headerAccessor the STOMP header accessor
     * @return User ID if available, null otherwise
     */
    Integer getUserIdFromWebSocketAuth(StompHeaderAccessor headerAccessor);

    // ==================== Authentication Status Methods ====================

    /**
     * Check if there is an authenticated user
     *
     * @return true if authenticated, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Check if the current user is a human user (has numeric ID)
     *
     * @return true if current user has numeric ID, false for system users or if not authenticated
     */
    boolean isHumanUser();

    // ==================== User Entity Methods ====================

    /**
     * Gets the human user entity if the user is human (has an id)
     *
     * @return User entity if found, null otherwise
     */
    User getCurrentHumanUser();

    // ==================== Role Methods ====================

    /**
     * Get the current user's role
     *
     * @return Role string if authenticated, null otherwise
     */
    String getCurrentUserRole();
}