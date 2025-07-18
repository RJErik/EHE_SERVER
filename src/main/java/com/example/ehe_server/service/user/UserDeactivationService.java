package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserDeactivationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserDeactivationService implements UserDeactivationServiceInterface {

    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public UserDeactivationService(
            UserRepository userRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> deactivateUser(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if user exists and is active
            User user = userContextService.getCurrentHumanUser();

            // Set the account status to suspended
            user.setAccountStatus(User.AccountStatus.SUSPENDED);
            userRepository.save(user);

            // Log the successful deactivation
            loggingService.logAction("User account deactivated successfully");

            // Return success response
            result.put("success", true);
            result.put("message", "Account deactivated successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError("Error deactivating user account: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while deactivating the account");
        }

        return result;
    }
}
