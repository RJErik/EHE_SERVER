package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.user.JwtTokenRenewalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;

    @Value("${jwt.refresh.expiration.time}")
    private long jwtRefreshExpirationTime;

    @Value("${jwt.refresh.max.expiration.time}")
    private long jwtRefreshTokenMaxExpireTime;

    public JwtTokenRenewalService(
            AdminRepository adminRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService) {
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
    }

    @Override
    @Transactional(timeout = 10)
    public void renewToken(Integer userId, HttpServletRequest request, HttpServletResponse response) {
        try {
            // Retrieve the currently authenticated user
            User user = userContextService.getCurrentHumanUser();

            // Determine if user has admin privileges
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            String role = "USER";
            if (isAdmin) {
                role = "ADMIN";
            }
            // Update the user context with current role information
            userContextService.setUser(String.valueOf(user.getUserId()), role);

            // Invalidate the old refresh token before generating new ones
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_refresh_token".equals(cookie.getName())) {
                        String refreshToken = cookie.getValue();
                        jwtRefreshTokenService.removeRefreshTokenByToken(refreshToken);
                        break;
                    }
                }
            } else {
                loggingService.logError("JWT token renewal failed, no cookies found.", new Throwable());
            }

            // Generate new JWT tokens
            String jwtAccessToken = jwtTokenGenerator.generateAccessToken(user.getUserId(), role);
            String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(user.getUserId(), role);

            // Set the new tokens in HTTP response cookies
            cookieService.addJwtAccessCookie(jwtAccessToken, response);
            cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

            // Hash and persist the new refresh token
            String refreshTokenHash = BCrypt.hashpw(jwtRefreshToken, BCrypt.gensalt());

            jwtRefreshTokenService.saveRefreshToken(
                    user,
                    refreshTokenHash,
                    jwtRefreshExpirationTime,
                    jwtRefreshTokenMaxExpireTime
            );

            loggingService.logAction("JWT token renewed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}