package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.UserResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserSearchServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserSearchService implements UserSearchServiceInterface {

    private final UserRepository userRepository;

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
                    "#size",
                    "#page",
                    "#result.content.size()"
            }
    )
    @Override
    public PaginatedResponse<UserResponse> searchUsers(
            Integer userId,
            String username,
            String email,
            User.AccountStatus accountStatus,
            LocalDateTime registrationDateToTime,
            LocalDateTime registrationDateFromTime,
            Integer size,
            Integer page) {

        // Data retrieval
        Pageable pageable = PageRequest.of(page, size);

        Page<User> userPage = userRepository.searchUsers(
                userId,
                (username != null && !username.trim().isEmpty() ? username : null),
                (email != null && !email.trim().isEmpty() ? email : null),
                accountStatus,
                registrationDateFromTime,
                registrationDateToTime,
                pageable
        );

        // Response mapping
        List<UserResponse> content = userPage.getContent().stream()
                .map(item -> new UserResponse(
                        item.getUserId(),
                        item.getUserName(),
                        item.getEmail(),
                        item.getAccountStatus(),
                        item.getRegistrationDate()
                ))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                userPage.getNumber(),
                userPage.getTotalPages(),
                content
        );
    }
}