package com.example.ehe_server.service.user;

import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserLogoutServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserLogoutService implements UserLogoutServiceInterface {

    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService) {
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
    }

    @Override
    public void logoutUser(Integer userId, HttpServletRequest request, HttpServletResponse response) {
        // Get the current user context before logout
        boolean userExists = false;
        boolean userActive = false;

        // Log the logout action with appropriate message
        if (!userExists) {
            loggingService.logAction("Logout attempted for non-existent user ID: " + userId);
        } else if (!userActive) {
            loggingService.logAction("Logout attempted for inactive user ID: " + userId);
        } else {
            loggingService.logAction("User logged out successfully");
        }

        // Extract refresh token from cookie and remove it from database
        for (Cookie cookie : request.getCookies()) {
            if ("jwt_refresh_token".equals(cookie.getName())) {
                String refreshToken = cookie.getValue();
                jwtRefreshTokenService.removeRefreshTokenByToken(refreshToken);
                break;
            }
        }

        // Clear JWT cookie regardless of user status
        cookieService.clearJwtCookies(response);
    }
}
