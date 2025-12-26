package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
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

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Execution
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(user);
    }
}