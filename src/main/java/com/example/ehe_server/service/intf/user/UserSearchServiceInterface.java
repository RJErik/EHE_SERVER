package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSearchServiceInterface {
    List<UserResponse> searchUsers(Integer userId,
                                   String username,
                                   String email,
                                   String accountStatus,
                                   LocalDateTime registrationDateToTime,
                                   LocalDateTime registrationDateFromTime);
}