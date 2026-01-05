package com.example.ehe_server.service.session;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.session.UserLogoutServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserLogoutService implements UserLogoutServiceInterface {

    private final CookieServiceInterface cookieService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService) {
        this.cookieService = cookieService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
    }

    @LogMessage(messageKey = "log.message.session.logout")
    @Override
    public void logoutUser(Integer userId, HttpServletRequest request, HttpServletResponse response) {

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
