package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserDeactivationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserDeactivationService implements UserDeactivationServiceInterface {

    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;

    public UserDeactivationService(
            UserRepository userRepository,
            LoggingServiceInterface loggingService) {
        this.userRepository = userRepository;
        this.loggingService = loggingService;
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        // Check if user exists and is active
        User user;
        if (userRepository.existsById(userId.intValue())) {
            user = userRepository.findById(userId.intValue()).get();
        } else {
            return;
        }

        // Set the account status to suspended
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(user);

        // Log the successful deactivation
        loggingService.logAction("User account deactivated successfully");
    }
}
