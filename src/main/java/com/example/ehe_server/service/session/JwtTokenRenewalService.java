package com.example.ehe_server.service.session;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.session.JwtTokenRenewalServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final UserContextService userContextService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final JwtProperties jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final LoggingServiceInterface loggingService;

    public JwtTokenRenewalService(
            AdminRepository adminRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            UserContextService userContextService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService,
            JwtProperties jwtConfig,
            PasswordEncoder passwordEncoder,
            LoggingServiceInterface loggingService) {
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
        this.loggingService = loggingService;
    }

    @LogMessage(messageKey = "log.message.session.jwtTokenRenewal")
    @Override
    public void renewToken(Integer userId, HttpServletRequest request, HttpServletResponse response) {
        // Retrieve the currently authenticated user
        User user = userContextService.getCurrentHumanUser();

        // Determine if user has admin privileges
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        String role = isAdmin ? "ADMIN" : "USER";
        // Update the user context with current role information
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        LocalDateTime anchorMaxExpiry = null;
        boolean tokenFoundInRequest = false;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_refresh_token".equals(cookie.getName())) {
                    String refreshToken = cookie.getValue();
                    tokenFoundInRequest = true;

                    // Attempt to remove the old token and retrieve its anchor date
                    anchorMaxExpiry = jwtRefreshTokenService.removeRefreshTokenByToken(refreshToken);

                    // REUSE DETECTION LOGIC:
                    // If we found a cookie, and the token signature was valid (passed filters),
                    // BUT removeRefreshTokenByToken returned null, it means the token is NOT in the DB.
                    // This implies the token was already rotated (stolen and used by hacker, or vice versa).
                    if (anchorMaxExpiry == null) {
                        loggingService.logAction("SECURITY ALERT: Refresh Token Reuse Detected for User ID: {}. Invalidating all sessions.");

                        // The Nuclear Option: Invalidate all tokens for this user to lock out the attacker
                        jwtRefreshTokenService.removeAllUserTokens(user.getUserId());

                        // Clear cookies on this response too
                        // cookieService.clearCookies(response); // specific clear method recommended

                        throw new RuntimeException("Security Alert: Invalid Refresh Token. Please log in again.");
                    }
                    break;
                }
            }
        }

        if (!tokenFoundInRequest) {
            // Handle missing token case appropriately
            throw new RuntimeException("No refresh token found in request");
        }

        // ABSOLUTE TIMEOUT CHECK:
        // Check if the "Anchor" date has passed. If so, the session chain is dead.
        if (LocalDateTime.now().isAfter(anchorMaxExpiry)) {
            loggingService.logAction("Session limit exceeded for User ID: {}. Forcing re-login.");
            throw new RuntimeException("Session limit exceeded. Please log in again.");
        }

        // Generate new JWT tokens
        String jwtAccessToken = jwtTokenGenerator.generateAccessToken(user.getUserId(), role);
        String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(user.getUserId(), role);

        // Set the new tokens in HTTP response cookies
        cookieService.addJwtAccessCookie(jwtAccessToken, response);
        cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

        // Hash and persist the new refresh token
        String refreshTokenHash = passwordEncoder.encode(jwtRefreshToken);

        // Save the new token, but INHERIT the old Max Expiry date
        jwtRefreshTokenService.saveRefreshToken(
                user,
                refreshTokenHash,
                jwtConfig.getJwtRefreshExpirationTime(), // Standard 7-day sliding window
                anchorMaxExpiry                          // Fixed 30-day absolute limit
        );
    }
}