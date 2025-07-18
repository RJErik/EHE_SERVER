package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserLogoutServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserLogoutService implements UserLogoutServiceInterface {

    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            UserRepository userRepository, UserContextService userContextService) {
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> logoutUser(HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Get the current user context before logout
            String userId = userContextService.getCurrentUserIdAsString();
            int userIdInt;
            boolean userExists = false;
            boolean userActive = false;

            // Try to parse user ID and check if user exists
            try {
                userIdInt = Integer.parseInt(userId);
                Optional<User> userOptional = userRepository.findById(userIdInt);

                if (userOptional.isPresent()) {
                    userExists = true;
                    User user = userOptional.get();
                    userActive = user.getAccountStatus() == User.AccountStatus.ACTIVE;
                }
            } catch (NumberFormatException e) {
                // userId was not a valid number, leave userIdInt as null
            }

            // Log the logout action with appropriate message
            if (!userExists) {
                loggingService.logAction("Logout attempted for non-existent user ID: " + userId);
            } else if (!userActive) {
                loggingService.logAction("Logout attempted for inactive user ID: " + userId);
            } else {
                loggingService.logAction("User logged out successfully");
            }

            // Clear JWT cookie regardless of user status
            cookieService.clearJwtCookie(response);

            // Always return success to the client
            responseBody.put("success", true);
            responseBody.put("message", "Successfully logged out");

        } catch (Exception e) {
            // Log the error
            loggingService.logError("Error during logout: " + e.getMessage(), e);

            // Still return success to client
            responseBody.put("success", true);
            responseBody.put("message", "Successfully logged out");
        }

        return responseBody;
    }
}
