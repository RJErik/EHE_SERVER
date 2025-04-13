package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserLogoutServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserLogoutService implements UserLogoutServiceInterface {

    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private final UserRepository userRepository;

    public UserLogoutService(
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            UserRepository userRepository) {
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> logoutUser(HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Get the current user context before logout
            String userId = auditContextService.getCurrentUser();
            Integer userIdInt = null;
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
                loggingService.logAction(userIdInt, userId, "Logout attempted for non-existent user ID: " + userId);
            } else if (!userActive) {
                loggingService.logAction(userIdInt, userId, "Logout attempted for inactive user ID: " + userId);
            } else {
                loggingService.logAction(userIdInt, userId, "User logged out successfully");
            }

            // Clear JWT cookie regardless of user status
            cookieService.clearJwtCookie(response);

            // Reset user context
            auditContextService.setCurrentUser("UNKNOWN");
            auditContextService.setCurrentUserRole("NONE");

            // Always return success to the client
            responseBody.put("success", true);
            responseBody.put("message", "Successfully logged out");

        } catch (Exception e) {
            // Log the error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error during logout: " + e.getMessage(), e);

            // Still return success to client
            responseBody.put("success", true);
            responseBody.put("message", "Successfully logged out");
        }

        return responseBody;
    }
}
