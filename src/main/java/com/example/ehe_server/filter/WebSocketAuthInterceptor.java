package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.auth.JwtClaimService;
import com.example.ehe_server.service.intf.auth.JwtClaimServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final JwtClaimServiceInterface jwtClaimService;

    public WebSocketAuthInterceptor(
            JwtTokenValidatorInterface jwtTokenValidator,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            JwtClaimServiceInterface jwtClaimService) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.jwtClaimService = jwtClaimService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Only authenticate on initial WebSocket connection
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateWebSocketConnection(accessor);
        }

        return message;
    }

    /**
     * Authenticates a WebSocket connection by extracting and validating JWT token.
     * Sets the authenticated user in both SecurityContext and STOMP session.
     */
    private void authenticateWebSocketConnection(StompHeaderAccessor accessor) {
        try {
            String token = extractTokenFromWebSocket(accessor);

            if (token == null) {
                loggingService.logAction("[WebSocket/CONNECT] No JWT token provided");
                return;
            }

            if (!jwtTokenValidator.validateAccessToken(token)) {
                loggingService.logAction("[WebSocket/CONNECT] Invalid or expired JWT token");
                return;
            }

            setWebSocketAuthentication(token, accessor);

        } catch (Exception e) {
            loggingService.logError("[WebSocket/CONNECT] Exception during authentication: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts JWT token from WebSocket connection.
     * First tries Authorization header, then falls back to session attributes.
     *
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromWebSocket(StompHeaderAccessor accessor) {
        // Try to get token from Authorization header (Bearer token format)
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.getFirst();
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // Remove "Bearer " prefix
            }
            return authHeader;
        }

        // Fallback: try session attributes (set during WebSocket handshake)
        if (accessor.getSessionAttributes() != null) {
            Object token = accessor.getSessionAttributes().get("jwt_access_token");
            if (token instanceof String) {
                return (String) token;
            }
        }

        return null;
    }

    /**
     * Validates token claims, loads user from DB, and sets authentication.
     * Sets authentication in both SecurityContext and STOMP accessor for accessibility
     * throughout the WebSocket session.
     */
    private void setWebSocketAuthentication(String token, StompHeaderAccessor accessor) {
        try {
            JwtClaimService.TokenDetails tokenDetails = jwtClaimService.parseTokenDetails(token);
            Integer userId = tokenDetails.userId();
            String role = tokenDetails.role();

            if (!areClaimsValid(userId, role)) {
                loggingService.logAction("[WebSocket/CONNECT] JWT claims invalid: userId=" + userId + ", role=" + role);
                return;
            }

            // Load and validate user exists and is active
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                loggingService.logAction("[WebSocket/CONNECT] JWT user not found: userId=" + userId);
                return;
            }

            User user = userOpt.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                loggingService.logAction("[WebSocket/CONNECT] JWT user inactive: userId=" + userId + ", status=" + user.getAccountStatus());
                return;
            }

            // ✅ Create Authentication and set in both contexts
            Authentication auth = createAuthentication(user, role);

            // Set in Spring Security context (for broader Spring access)
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Set in STOMP session (for WebSocket-specific access)
            accessor.setUser(auth);

            loggingService.logAction("[WebSocket/CONNECT] User authenticated: userId=" + user.getUserId() + ", role=" + role);

        } catch (Exception e) {
            loggingService.logError("[WebSocket/CONNECT] Exception setting WebSocket authentication: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an Authentication object with the user's role as a granted authority.
     * Spring Security uses this to check message-level authorization (e.g., @PreAuthorize).
     */
    private Authentication createAuthentication(User user, String role) {
        String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return new UsernamePasswordAuthenticationToken(
                user.getUserId(),  // principal: who is this?
                null,              // credentials: not needed (token already validated)
                Collections.singletonList(new SimpleGrantedAuthority(authorityName))
        );
    }

    /**
     * Validates that userId and role claims are present and valid.
     */
    private boolean areClaimsValid(Integer userId, String role) {
        return userId != null && role != null && !role.trim().isEmpty();
    }
}