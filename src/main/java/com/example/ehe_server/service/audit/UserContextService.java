package com.example.ehe_server.service.audit;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage user context throughout the application.
 * Supports both HTTP (SecurityContextHolder) and WebSocket (StompHeaderAccessor) authentication.
 */
@Component
public class UserContextService implements UserContextServiceInterface {

    private final UserRepository userRepository;

    // ThreadLocal to store WebSocket header accessor for the current thread
    private static final ThreadLocal<StompHeaderAccessor> webSocketContext = new ThreadLocal<>();

    public UserContextService(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ==================== WebSocket Context Management ====================

    /**
     * Set the WebSocket header accessor for the current thread.
     * Call this at the start of WebSocket message handling.
     *
     * @param headerAccessor the STOMP header accessor from the WebSocket message
     */
    public void setWebSocketContext(StompHeaderAccessor headerAccessor) {
        webSocketContext.set(headerAccessor);
    }

    /**
     * Clear the WebSocket context for the current thread.
     * Call this after WebSocket message handling is complete.
     */
    public void clearWebSocketContext() {
        webSocketContext.remove();
    }

    // ==================== Authentication Methods ====================

    /**
     * Set the current user in SecurityContextHolder
     */
    public void setUser(String userId, String role) {
        String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(prefixedRole)
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Get the current Authentication object.
     * First tries SecurityContextHolder, then falls back to WebSocket context.
     *
     * @return Authentication object if available, null otherwise
     */
    private Authentication getCurrentAuthentication() {
        // Try SecurityContextHolder first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isValidAuthentication(authentication)) {
            return authentication;
        }

        // Fall back to WebSocket context
        StompHeaderAccessor headerAccessor = webSocketContext.get();
        if (headerAccessor != null) {
            return (Authentication) headerAccessor.getUser();
        }

        return null;
    }

    /**
     * Check if an Authentication object is valid (non-null, authenticated, with String principal)
     */
    private boolean isValidAuthentication(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null;
    }

    // ==================== User ID Methods ====================

    /**
     * Get the current authenticated user ID as Integer
     *
     * @return User ID if authenticated and numeric, null otherwise
     */
    public Integer getCurrentUserId() {
        if (isHumanUser()) {
            try {
                return Integer.parseInt(getCurrentUserIdAsString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get the current user's ID as a string.
     * Supports multiple principal types from both HTTP and WebSocket contexts.
     *
     * @return User ID string, "UNKNOWN" if not authenticated
     */
    public String getCurrentUserIdAsString() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null) {
            return "UNKNOWN";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String) {
            return (String) principal;
        }

        return "UNKNOWN";
    }

    /**
     * Get the current user ID directly from a WebSocket header accessor.
     * Useful when you have direct access to the header accessor.
     *
     * @param headerAccessor the STOMP header accessor
     * @return User ID if available, null otherwise
     */
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
                return null;
            }
        }

        return null;
    }

    // ==================== Authentication Status Methods ====================

    /**
     * Check if there is an authenticated user
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return getCurrentAuthentication() != null;
    }

    /**
     * Check if the current user is a human user (has numeric ID)
     *
     * @return true if current user has numeric ID, false for system users or if not authenticated
     */
    public boolean isHumanUser() {
        String userIdStr = getCurrentUserIdAsString();
        if (userIdStr == null || "UNKNOWN".equals(userIdStr)) {
            return false;
        }

        try {
            Integer.parseInt(userIdStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==================== User Entity Methods ====================

    /**
     * Gets the human user entity if the user is human (has an id)
     *
     * @return User entity if found, null otherwise
     */
    public User getCurrentHumanUser() {
        if (isHumanUser()) {
            Optional<User> user = userRepository.findById(getCurrentUserId());
            return user.orElse(null);
        }
        return null;
    }

    // ==================== Role Methods ====================

    /**
     * Get the current user's role
     *
     * @return Role string if authenticated, null otherwise
     */
    public String getCurrentUserRole() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }
}