package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserInfoServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserInfoService implements UserInfoServiceInterface {

    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public UserInfoService(
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();

            // Return user info if all checks passed
            result.put("success", true);
            result.put("userName", user.getUserName());
            result.put("email", user.getEmail());

            // Log the successful user info retrieval
            loggingService.logAction("User info retrieved successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError("Error retrieving user info: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving user information");
        }

        return result;
    }
}
