package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.UserResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserRetrievalServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserRetrievalService implements UserRetrievalServiceInterface {
    private final UserRepository userRepository;

    public UserRetrievalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.get",
            params = {
                    "#size",
                    "#page",
                    "#result.content.size()"
            }
    )
    @Override
    public PaginatedResponse<UserResponse> getUsers(Integer size, Integer page) {
        // Get all users excluding admins, ordered by registration date
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAllNonAdminsOrderByRegistrationDateDesc(pageable);

        // Transform entities to DTOs
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