package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserInfoServiceInterface;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserInfoService implements UserInfoServiceInterface {

    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public UserInfoService(
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get the current user from the database
            Optional<User> userOptional = userRepository.findById(userId.intValue());

            // Check if the user exists
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userId.toString(), "User info retrieval failed: User not found");
                return result;
            }

            User user = userOptional.get();

            // Check if the account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId.intValue(), userId.toString(),
                        "User info retrieval failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Return user info if all checks passed
            result.put("success", true);
            result.put("userName", user.getUserName());
            result.put("email", user.getEmail());

            // Log the successful user info retrieval
            loggingService.logAction(userId.intValue(), userId.toString(), "User info retrieved successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving user info: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving user information");
        }

        return result;
    }
}
