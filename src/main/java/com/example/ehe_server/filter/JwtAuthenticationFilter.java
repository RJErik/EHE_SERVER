package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.security.JwtTokenValidatorInterface;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenValidatorInterface jwtTokenValidator,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip filter for public endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        try {
            // Validate token and set Authentication if valid
            if (token != null && jwtTokenValidator.validateToken(token)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                List<String> roles = jwtTokenValidator.getRolesFromToken(token);

                if (userId != null && !roles.isEmpty()) {
                    // Convert roles to Spring Security authorities
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    authorities
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Log access to restricted endpoints
                    if (isSignificantEndpoint(path)) {
                        Optional<User> userOpt = userRepository.findById(userId.intValue());
                        if (userOpt.isPresent()) {
                            loggingService.logAction(userOpt.get(), "Accessed " + path);
                        }
                    }
                } else if (isSignificantEndpoint(path)) {
                    loggingService.logAction("Invalid token contents in request to " + path);
                }
            } else if (token != null && isSignificantEndpoint(path)) {
                loggingService.logAction("Invalid token in request to " + path);
            }
        } catch (Exception e) {
            if (isSignificantEndpoint(path)) {
                loggingService.logError("Error validating token for " + path, e);
            }
        }

        filterChain.doFilter(request, response);
    }

    // Helper method to determine if this is an endpoint we want to log access for
    private boolean isSignificantEndpoint(String path) {
        return path.startsWith("/api/admin/") ||
                path.contains("/profile") ||
                path.contains("/settings");
    }
}