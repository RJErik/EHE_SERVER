package com.example.ehe_server.service.user;

import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserLogoutServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserLogoutService implements UserLogoutServiceInterface {

    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService) {
        this.cookieService = cookieService;
        this.loggingService = loggingService;
    }

    @Override
    public void logoutUser(Integer userId, HttpServletResponse response) {
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

        // Clear JWT cookie regardless of user status
        cookieService.clearJwtCookies(response);
    }
}
