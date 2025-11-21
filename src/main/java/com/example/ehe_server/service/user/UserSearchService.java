package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.UserSearchResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserSearchService implements UserSearchServiceInterface {

    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.search",
            params = {
                    "#userId",
                    "#username",
                    "#email",
                    "#accountStatus",
                    "#registrationDateToTime",
                    "#registrationDateFromTime",
                    "#result.size()"
            }
    )

    @Override
    public List<UserSearchResponse> searchUsers(Integer userId, String username, String email, String accountStatus, LocalDateTime registrationDateToTime, LocalDateTime registrationDateFromTime) {

        // Convert string enum values to actual enums
        User.AccountStatus userStatus = null;
        if (accountStatus != null && !accountStatus.trim().isEmpty()) {
            try {
                userStatus = User.AccountStatus.valueOf(accountStatus);
            } catch (IllegalArgumentException e) {
                //Todo throw error
                return List.of();
            }
        }

        List<User> users = userRepository.searchUsers(
                userId,
                (username != null && !username.trim().isEmpty() ? username : null),
                (email != null && !email.trim().isEmpty() ? email : null),
                userStatus,
                registrationDateToTime,
                registrationDateFromTime);

        return users.stream()
                .map(item -> new UserSearchResponse(
                        item.getUserId(),
                        item.getUserName(),
                        item.getEmail(),
                        item.getAccountStatus().name(),
                        item.getRegistrationDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

    }
}
