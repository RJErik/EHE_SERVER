package ehe_server.service.user;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.UserInfoResponse;
import ehe_server.entity.User;
import ehe_server.exception.custom.UserNotFoundException;
import ehe_server.repository.UserRepository;
import ehe_server.service.intf.user.UserInfoServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserInfoService implements UserInfoServiceInterface {

    private final UserRepository userRepository;

    public UserInfoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @LogMessage(messageKey = "log.message.user.userInfo.get")
    @Override
    public UserInfoResponse getUserInfo(Integer userId) {

        // Data retrieval
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Response mapping
        return new UserInfoResponse(user.getUserName(), user.getEmail());
    }
}