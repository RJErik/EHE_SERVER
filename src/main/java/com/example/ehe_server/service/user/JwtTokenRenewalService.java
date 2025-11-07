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
@Transactional
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
    public void renewToken(Long userId, HttpServletRequest request, HttpServletResponse response) {
        // Get current user ID from user context
        User user = userContextService.getCurrentHumanUser();

        // Get the user's role
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }

        // Update audit context with authenticated user and roles
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        for (Cookie cookie : request.getCookies()) {
            if ("jwt_refresh_token".equals(cookie.getName())) {
                String refreshToken = cookie.getValue();
                jwtRefreshTokenService.removeRefreshTokenByHash(BCrypt.hashpw(refreshToken, BCrypt.gensalt()));
                break;
            }
        }

        String jwtAccessToken = jwtTokenGenerator.generateAccessToken(Long.valueOf(user.getUserId()), role);
        String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(Long.valueOf(user.getUserId()), role);

        // Create JWT cookie
        cookieService.addJwtAccessCookie(jwtAccessToken, response);
        cookieService.addJwtRefreshCookie(jwtRefreshToken, response);

        // Use the new service to save the refresh token
        String refreshTokenHash = BCrypt.hashpw(jwtRefreshToken, BCrypt.gensalt());
        jwtRefreshTokenService.saveRefreshToken(
                user,
                refreshTokenHash,
                jwtRefreshExpirationTime,
                jwtRefreshTokenMaxExpireTime
        );



        // Log the successful token renewal
        loggingService.logAction("JWT token renewed successfully");
    }
}
