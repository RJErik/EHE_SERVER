package com.example.ehe_server.service.audit;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage user context throughout the application
 */
@Component
public class UserContextService implements UserContextServiceInterface {

    // Thread-local storage for request-specific information
    private static final ThreadLocal<String> requestPath = new ThreadLocal<>();
    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     *  Set the current user
     */
    public void setUser(String userId, String role) {
        // Convert role to Spring Security authority
        String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(prefixedRole)
        );

        // Create authentication token with user details
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Set the current request path
     */
    public void setRequestPath(String path) {
        requestPath.set(path);
    }

    /**
     * Get the current request path
     */
    public String getRequestPath() {
        return requestPath.get();
    }

    /**
     * Get the current authenticated user ID
     * @return User ID if authenticated, null otherwise
     */
    public Long getCurrentUserId() {
        if (isHumanUser()) {
            try {
                return Long.parseLong(getCurrentUserIdAsString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get the current user's username for PostgreSQL audit context
     * @return Username string, "UNKNOWN" if not authenticated
     */
    public String getCurrentUserIdAsString() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return "UNKNOWN";
    }

    /**
     * Check if there is an authenticated user
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String;
    }

    /**
     * Check if the current user is a human user (has numeric ID)
     * @return true if current user has numeric ID, false for system users or if not authenticated
     */
    public boolean isHumanUser() {
        String userIdStr = getCurrentUserIdAsString();
        if (userIdStr == null) {
            return false;
        }

        try {
            Long.parseLong(userIdStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Gets the human user if the user is human (has an id)
     * @return with the user attribute, otherwise return null
     */
    public User getCurrentHumanUser() {
        if (isHumanUser()) {
            Optional<User> user = userRepository.findById(getCurrentUserId().intValue());
            if (user.isPresent()) {
                return user.get();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the current user's role
     * @return Role string if authenticated, null otherwise
     */
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }
}