package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final UserRepository userRepository; // <-- Added UserRepository
    private final LoggingServiceInterface loggingService; // <-- Added LoggingService

    public WebSocketAuthInterceptor(
            JwtTokenValidatorInterface jwtTokenValidator,
            UserRepository userRepository,
            LoggingServiceInterface loggingService) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // We only care about the initial connection command
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            // Try to get token from STOMP header first
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                token = authorization.get(0).replace("Bearer ", "");
            }

            // If not found in header, try to get from session attributes (from handshake)
            if (token == null && accessor.getSessionAttributes() != null) {
                token = (String) accessor.getSessionAttributes().get("jwt_token");
            }

            // Step 1: Validate the token's structure and signature
            if (token != null && jwtTokenValidator.validateToken(token)) {
                try {
                    Long userId = jwtTokenValidator.getUserIdFromToken(token);
                    String role = jwtTokenValidator.getRoleFromToken(token);

                    if (userId == null || role == null || role.trim().isEmpty()) {
                        loggingService.logAction("WebSocket Auth Failed: Invalid userId or role in JWT token.");
                        return message; // Stop further processing
                    }

                    // Step 2: Check if user exists in the database
                    Optional<User> userOpt = userRepository.findById(userId.intValue());

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Step 3: Check if the user's account is active
                        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                            // All checks passed, user is authenticated and active.
                            String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userId, // Principal
                                            null,   // Credentials
                                            Collections.singletonList(new SimpleGrantedAuthority(prefixedRole))
                                    );
                            // Attach the authenticated user to the WebSocket session
                            accessor.setUser(authentication);
                            loggingService.logAction("WebSocket connection established for user: " + userId);
                        } else {
                            // User exists but account is not active
                            loggingService.logAction("WebSocket Auth Failed: User account status is " + user.getAccountStatus() + " for user: " + userId);
                        }
                    } else {
                        // User from token does not exist in the database
                        loggingService.logAction("WebSocket Auth Failed: User not found in database for user ID: " + userId);
                    }
                } catch (Exception e) {
                    loggingService.logError("Exception during WebSocket authentication: " + e.getMessage(), e);
                }
            } else {
                // Token is invalid or missing
                if (token != null) {
                    loggingService.logAction("WebSocket Auth Failed: Invalid JWT token provided.");
                }
            }
        }
        return message;
    }
}