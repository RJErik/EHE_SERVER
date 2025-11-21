package com.example.ehe_server.filter;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.intf.auth.JwtClaimServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@Order(1)  // Run BEFORE Spring Security's authorization layer
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidatorInterface jwtTokenValidator;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;
    private final JwtProperties jwtConfig;
    private final JwtClaimServiceInterface jwtClaimService;


    private enum TokenType {
        ACCESS("jwt_access_token"),
        REFRESH("jwt_refresh_token");

        private final String cookieName;

        TokenType(String cookieName) {
            this.cookieName = cookieName;
        }

        public String getCookieName() {
            return cookieName;
        }
    }

    public JwtAuthenticationFilter(
            JwtTokenValidatorInterface jwtTokenValidator,
            LoggingServiceInterface loggingService,
            UserRepository userRepository,
            JwtProperties jwtConfig,
            JwtClaimServiceInterface jwtClaimService) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
        this.jwtConfig = jwtConfig;
        this.jwtClaimService = jwtClaimService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Try to authenticate the user (if token exists)
        // Doesn't matter if it fails or succeeds—SecurityContext will be populated or empty
        // Spring Security's authorizeHttpRequests will handle what to do
        authenticateIfTokenExists(request, path);

        filterChain.doFilter(request, response);
    }

    /**
     * Attempts to extract, validate, and set user authentication in SecurityContext.
     * If no valid token exists, SecurityContext remains empty (which is fine—
     * Spring Security will treat the request as unauthenticated).
     */
    private void authenticateIfTokenExists(HttpServletRequest request, String path) {
        try {
            // Try access token first (normal requests)
            String token = extractTokenFromCookie(TokenType.ACCESS, request);

            if (token != null && isTokenValid(token, TokenType.ACCESS)) {
                setUserAuthentication(token, path);
                return;
            }

            // If access token failed, maybe it's a refresh endpoint—try refresh token
            String refreshToken = extractTokenFromCookie(TokenType.REFRESH, request);
            if (refreshToken != null && isTokenValid(refreshToken, TokenType.REFRESH) && path.equals(jwtConfig.getJwtRefreshUrl())) {
                setUserAuthentication(refreshToken, path);
                return;
            }

            // No valid token found—that's OK, SecurityContext stays empty
            loggingService.logAction("[" + path + "] No valid JWT token provided");

        } catch (Exception e) {
            loggingService.logError("[" + path + "] Error during JWT authentication: " + e.getMessage(), e);
            // Don't throw—let SecurityConfig's authorizeHttpRequests handle rejection
        }
    }

    /**
     * Validates token, extracts claims, loads User from DB, and sets Authentication.
     */
    private void setUserAuthentication(String token, String path) {
        try {
            Integer userId = jwtClaimService.getUserIdFromToken(token);
            String role = jwtClaimService.getRoleFromToken(token);

            if (!areClaimsValid(userId, role)) {
                loggingService.logAction("[" + path + "] JWT claims invalid: userId=" + userId + ", role=" + role);
                return;
            }

            // Load and validate user exists and is active
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                loggingService.logAction("[" + path + "] JWT user not found: userId=" + userId);
                return;
            }

            User user = userOpt.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                loggingService.logAction("[" + path + "] JWT user inactive: userId=" + userId + ", status=" + user.getAccountStatus());
                return;
            }

            // ✅ Create Authentication with roles and set in context
            Authentication auth = createAuthentication(user, role);
            SecurityContextHolder.getContext().setAuthentication(auth);

            loggingService.logAction("[" + path + "] User authenticated: userId=" + user.getUserId() + ", role=" + role);

        } catch (Exception e) {
            loggingService.logError("[" + path + "] Exception setting user authentication: " + e.getMessage(), e);
        }
    }

    /**
     * Creates an Authentication object with the user's role as a granted authority.
     * Spring Security's authorizeHttpRequests will read this to check hasRole().
     */
    private Authentication createAuthentication(User user, String role) {
        // Convert role string to GrantedAuthority
        // Spring expects "ROLE_ADMIN", "ROLE_USER", etc.
        String authorityName = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return new PreAuthenticatedAuthenticationToken(
                String.valueOf(user.getUserId()),  // principal: who is this?
                null,                              // credentials: not needed (token already validated)
                Collections.singletonList(new SimpleGrantedAuthority(authorityName))
        );
    }

    private String extractTokenFromCookie(TokenType tokenType, HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (tokenType.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isTokenValid(String token, TokenType tokenType) {
        return tokenType == TokenType.ACCESS
                ? jwtTokenValidator.validateAccessToken(token)
                : jwtTokenValidator.validateRefreshToken(token);
    }

    private boolean areClaimsValid(Integer userId, String role) {
        return userId != null && role != null && !role.trim().isEmpty();
    }
}