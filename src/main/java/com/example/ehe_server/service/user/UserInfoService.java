package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.UserInfoResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.UserInfoServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserInfoService implements UserInfoServiceInterface {

    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public UserInfoService(
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {

        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId.intValue())) {
            user = userRepository.findById(userId.intValue()).get();
        } else {
            return null;
        }

        // Log the successful user info retrieval
        loggingService.logAction("User info retrieved successfully");
        // Return user info in a DTO
        return new UserInfoResponse(user.getUserName(), user.getEmail());
    }
}
