package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.UserRetrievalResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.UserRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserRetrievalService implements UserRetrievalServiceInterface {
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserRetrievalService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.get",
            params = {"#result.size()"}
    )
    @Override
    public List<UserRetrievalResponse> getUsers() {
        List<User> users = userRepository.findAll();

        // Transform entities to DTOs
        return users.stream()
                .map(item -> new UserRetrievalResponse(
                        item.getUserId(),
                        item.getUserName(),
                        item.getEmail(),
                        item.getAccountStatus().toString(),
                        item.getRegistrationDate().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}
