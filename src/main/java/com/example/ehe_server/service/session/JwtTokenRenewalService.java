package com.example.ehe_server.service.session;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.session.JwtTokenRenewalServiceInterface;
import com.example.ehe_server.service.intf.token.TokenHashServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final UserContextService userContextService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;
    private final JwtProperties jwtConfig;
    private final LoggingServiceInterface loggingService;
    private final TokenHashServiceInterface tokenHashService;

    public JwtTokenRenewalService(
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            UserContextService userContextService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService,
            JwtProperties jwtConfig,
            LoggingServiceInterface loggingService,
            TokenHashServiceInterface tokenHashService) {
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.jwtConfig = jwtConfig;
        this.loggingService = loggingService;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(messageKey = "log.message.session.jwtTokenRenewal")
    @Override
    public void renewToken(Integer userId, HttpServletRequest request, HttpServletResponse response) {

        LocalDateTime anchorMaxExpiry = null;
        boolean tokenFoundInRequest = false;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_refresh_token".equals(cookie.getName())) {
                    String refreshToken = cookie.getValue();
                    tokenFoundInRequest = true;

                    anchorMaxExpiry = jwtRefreshTokenService.removeRefreshTokenByToken(refreshToken);

                    // If we found a cookie, and the token signature was valid (passed filters),
                    // BUT removeRefreshTokenByToken returned null, it means the token is NOT in the DB.
                    // This implies the token was already rotated (stolen and used by hacker, or vice versa).
                    if (anchorMaxExpiry == null) {
                        loggingService.logAction("Refresh Token Reuse Detected.. Invalidating all sessions.");

                        // The Nuclear Option: Invalidate all tokens for this user to lock out the attacker
                        jwtRefreshTokenService.removeAllUserTokens(userContextService.getCurrentUserId());

                        cookieService.clearJwtCookies(response);

                        throw new RuntimeException("Invalid Refresh Token");
                    }
                    break;
                }
            }
        }

        if (!tokenFoundInRequest) {
            // Handle missing token case appropriately
            throw new RuntimeException("No refresh token found in request");
        }

        // Check if the "Anchor" date has passed. If so, the session chain is dead.
        if (LocalDateTime.now().isAfter(anchorMaxExpiry)) {
            loggingService.logAction("Session limit exceeded.. Forcing re-login.");
            throw new RuntimeException("Session limit exceeded. Please log in again.");
        }

        // Generate new JWT tokens
        String jwtAccessToken = jwtTokenGenerator.generateAccessToken(userContextService.getCurrentUserId(), userContextService.getCurrentUserRole());
        String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(userContextService.getCurrentUserId(), userContextService.getCurrentUserRole());

        // Set the new tokens in HTTP response cookies
        cookieService.addJwtAccessCookie(jwtAccessToken, response);
        cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

        // Hash and persist the new refresh token
        String refreshTokenHash = tokenHashService.hashToken(jwtRefreshToken);

        // Save the new token, but INHERIT the old Max Expiry date
        jwtRefreshTokenService.saveRefreshToken(
                userContextService.getCurrentHumanUser(),
                refreshTokenHash,
                jwtConfig.getJwtRefreshExpirationTime(),
                anchorMaxExpiry
        );
    }
}