package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserDeactivationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserDeactivationService implements UserDeactivationServiceInterface {

    private final UserRepository userRepository;

    public UserDeactivationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @LogMessage(messageKey = "log.message.user.userDeactivation")
    @Override
    public void deactivateUser(Integer userId) {
        // Check if user exists and is active
        User user;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            return;
        }

        // Set the account status to suspended
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(user);
    }
}
