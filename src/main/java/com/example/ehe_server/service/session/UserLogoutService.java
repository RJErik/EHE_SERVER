package com.example.ehe_server.service.session;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.UserRepository;
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
    private final UserRepository userRepository;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService,
            UserRepository userRepository) {
        this.cookieService = cookieService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
        this.userRepository = userRepository;
    }

    @LogMessage(messageKey = "log.message.session.logout")
    @Override
    public void logoutUser(Integer userId, HttpServletRequest request, HttpServletResponse response) {
        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
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
