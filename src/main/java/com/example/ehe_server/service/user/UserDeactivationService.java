package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserDeactivationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserDeactivationService implements UserDeactivationServiceInterface {

    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public UserDeactivationService(
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> deactivateUser(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get the current user from the database
            Optional<User> userOptional = userRepository.findById(userId.intValue());

            // Check if the user exists
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userId.toString(), "User deactivation failed: User not found");
                return result;
            }

            User user = userOptional.get();

            // Check if the account is already inactive
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is already inactive");
                loggingService.logAction(userId.intValue(), userId.toString(),
                        "User deactivation failed: Account already inactive, status=" + user.getAccountStatus());
                return result;
            }

            // Set the account status to suspended
            user.setAccountStatus(User.AccountStatus.SUSPENDED);
            userRepository.save(user);

            // Log the successful deactivation
            loggingService.logAction(userId.intValue(), userId.toString(), "User account deactivated successfully");

            // Return success response
            result.put("success", true);
            result.put("message", "Account deactivated successfully");

        } catch (Exception e) {
            // Log any errors
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error deactivating user account: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while deactivating the account");
        }

        return result;
    }
}
