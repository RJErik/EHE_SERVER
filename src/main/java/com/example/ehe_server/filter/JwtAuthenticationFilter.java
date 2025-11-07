package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final CookieServiceInterface cookieService;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;

    @Value("${jwt.refresh.url}")
    private String REFRESH_URL;

    // Be more specific about protected paths - exact matching for endpoints
    private static final List<String> PROTECTED_PATHS = List.of(
            "/api/user/",
            "/api/admin/",
            "/candles/"
            // Add other protected paths
    );

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/api/home/"
    );

    public JwtAuthenticationFilter(
            JwtTokenValidatorInterface jwtTokenValidator,
            LoggingServiceInterface loggingService,
            UserRepository userRepository,
            UserContextService userContextService,
            CookieServiceInterface cookieService,
            JwtTokenGeneratorInterface jwtTokenGenerator) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.cookieService = cookieService;
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Set the request path in audit context
        userContextService.setRequestPath(request.getRequestURI());

        // Check if this is a public endpoint
        boolean isPublicEndpoint = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        // Skip authentication for public endpoints
        if (isPublicEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        // **NEW: Special handling for refresh endpoint**
        if (path.equals(REFRESH_URL)) {
            handleRefreshEndpoint(request, response, filterChain);
            return;
        }

        // Extract JWT access token from cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        boolean isAuthenticated = false;

        try {
            // First validate the JWT token
            if (token != null && jwtTokenValidator.validateAccessToken(token)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                String role = jwtTokenValidator.getRoleFromToken(token);

                // Check if both userId and role are valid
                if (userId != null && role != null && !role.trim().isEmpty()) {

                    // Now check if user exists and is active in the database
                    Optional<User> userOpt = userRepository.findById(userId.intValue());

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Check if account is active
                        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                            userContextService.setUser(String.valueOf(user.getUserId()), role);
                            isAuthenticated = true;

                            // Log access to significant endpoints
                            if (isSignificantEndpoint(path)) {
                                loggingService.logAction("Accessed " + path);
                            }

                            cookieService.clearJwtCookies(response);
                            cookieService.addJwtAccessCookie(jwtTokenGenerator.generateAccessToken(user.getUserId().longValue(), role), response);
                        } else {
                            // User exists but is not active
                            loggingService.logAction("Authentication failed: User account status is " + user.getAccountStatus());
                        }
                    } else {
                        // User doesn't exist in database
                        loggingService.logAction("Authentication failed: User not found in database");
                    }
                } else {
                    // Invalid userId or role in token
                    loggingService.logAction("Authentication failed: Invalid userId or role in JWT token");
                }
            } else {
                // Invalid or missing token
                if (token != null) {
                    loggingService.logAction("Authentication failed: Invalid JWT token");
                }
            }
        } catch (Exception e) {
            loggingService.logError("Exception during authentication: " + e.getMessage(), e);
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

        // If protected path and not authenticated, return 401
        if (needsAuthentication && !isAuthenticated) {
            loggingService.logAction("Unauthorized access attempt to " + path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"unauthorized\", \"message\": \"Invalid or expired access token\"}");
            return;
        }

        // Continue with filter chain
        filterChain.doFilter(request, response);
    }

    // **NEW: Add this method to handle refresh endpoint**
    private void handleRefreshEndpoint(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract refresh token from cookie
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        try {
            // Validate the refresh token
            if (refreshToken != null && jwtTokenValidator.validateRefreshToken(refreshToken)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(refreshToken);
                String role = jwtTokenValidator.getRoleFromToken(refreshToken);

                // Check if both userId and role are valid
                if (userId != null && role != null && !role.trim().isEmpty()) {

                    // Check if user exists and is active in the database
                    Optional<User> userOpt = userRepository.findById(userId.intValue());

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // Check if account is active
                        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                            // Set user context
                            userContextService.setUser(String.valueOf(user.getUserId()), role);

                            loggingService.logAction("Valid refresh token for user " + userId);

                            // Allow the request to proceed to the refresh controller
                            filterChain.doFilter(request, response);
                            return;
                        } else {
                            loggingService.logAction("Refresh failed: User account status is " + user.getAccountStatus());
                        }
                    } else {
                        loggingService.logAction("Refresh failed: User not found in database");
                    }
                } else {
                    loggingService.logAction("Refresh failed: Invalid userId or role in refresh token");
                }
            } else {
                loggingService.logAction("Refresh failed: Invalid or missing refresh token");
            }
        } catch (Exception e) {
            loggingService.logError("Exception during refresh token validation: " + e.getMessage(), e);
        }

        // If we reach here, refresh token validation failed - return 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"unauthorized\", \"message\": \"Invalid or expired refresh token\"}");
    }

    // Helper method to determine if this is an endpoint we want to log access for
    private boolean isSignificantEndpoint(String path) {
        return path.startsWith("/api/admin/") ||
                path.contains("/profile") ||
                path.contains("/settings") ||
                path.contains("/user");
    }
}