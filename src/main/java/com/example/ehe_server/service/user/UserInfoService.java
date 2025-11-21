package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.UserInfoResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserInfoServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserInfoService implements UserInfoServiceInterface {

    private final UserRepository userRepository;

    public UserInfoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @LogMessage(messageKey = "log.message.user.userInfo.get")
    @Override
    public UserInfoResponse getUserInfo(Integer userId) {

        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            return null;
        }

        // Return user info in a DTO
        return new UserInfoResponse(user.getUserName(), user.getEmail());
    }
}
