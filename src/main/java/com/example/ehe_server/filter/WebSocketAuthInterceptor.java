package com.example.ehe_server.filter;

import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
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

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenValidatorInterface jwtTokenValidator;

    public WebSocketAuthInterceptor(JwtTokenValidatorInterface jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            // Try to get token from STOMP header first (for non-browser clients or testing)
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                token = authorization.get(0).replace("Bearer ", "");
            }

            // If not found in header, try to get from session attributes (from handshake)
            if (token == null && accessor.getSessionAttributes() != null) {
                token = (String) accessor.getSessionAttributes().get("jwt_token");
            }

            if (token != null && jwtTokenValidator.validateToken(token)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                String role = jwtTokenValidator.getRoleFromToken(token);

                // Set authentication for this connection
                String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(prefixedRole))
                        );
                accessor.setUser(authentication);
            }
        }
        return message;
    }
}
