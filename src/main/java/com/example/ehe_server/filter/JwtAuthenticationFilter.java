package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;
    private final AuditContextService auditContextService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Be more specific about protected paths - exact matching for endpoints
    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/user/",
            "/api/admin/"
            // Add other protected paths
    );

    public JwtAuthenticationFilter(
            JwtTokenValidatorInterface jwtTokenValidator,
            LoggingServiceInterface loggingService,
            UserRepository userRepository,
            AuditContextService auditContextService) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
        this.auditContextService = auditContextService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Set the request path in audit context (still useful)
        auditContextService.setRequestPath(request.getRequestURI());

        // Skip filter for public endpoints
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

        boolean isAuthenticated = false;
        try {
            // Validate token and set Spring Security context if valid
            if (token != null && jwtTokenValidator.validateToken(token)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                String role = jwtTokenValidator.getRoleFromToken(token);

                // Check if both userId and role are valid
                if (userId != null && role != null && !role.trim().isEmpty()) {
                    // Convert role to Spring Security authority
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

                    // IMPORTANT: No longer setting PostgreSQL context here
                    // This will be handled by UserContextService at the controller level

                    isAuthenticated = true;

                    // Log access to significant endpoints
                    if (isSignificantEndpoint(path)) {
                        Optional<User> userOpt = userRepository.findById(userId.intValue());
                        if (userOpt.isPresent()) {
                            loggingService.logAction(userId.intValue(), userId.toString(), "Accessed " + path);
                        }
                    }
                }
            }
        } catch (Exception e) {
            loggingService.logError(null, "system", "Exception during authentication: " + e.getMessage(), e);
        }

        // Check if this is a protected path
        boolean needsAuthentication = false;

        // Handle exact endpoints
        if (path.equals("/api/user/verify") || path.equals("/api/admin/verify")) {
            needsAuthentication = true;
        } else {
            // For other paths, check if they start with any protected path
            needsAuthentication = PROTECTED_PATHS.stream()
                    .anyMatch(path::startsWith);
        }

        // If protected path and not authenticated, redirect to login
        if (needsAuthentication && !isAuthenticated) {
            loggingService.logAction(null, "system", "Redirecting unauthenticated user from " + path + " to login page");
            response.setStatus(HttpServletResponse.SC_FOUND); // 302 Found
            response.setHeader("Location", frontendUrl);
            return;
        }

        // Continue with filter chain
        filterChain.doFilter(request, response);
    }

    // Helper method to determine if this is an endpoint we want to log access for
    private boolean isSignificantEndpoint(String path) {
        return path.startsWith("/api/admin/") ||
                path.contains("/profile") ||
                path.contains("/settings") ||
                path.contains("/user");
    }
}
