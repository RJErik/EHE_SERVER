package ehe_server.service.user;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.User;
import ehe_server.exception.custom.UserNotFoundException;
import ehe_server.repository.UserRepository;
import ehe_server.service.intf.user.UserDeactivationServiceInterface;
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

        // Data retrieval
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Execution
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
    }
}