package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.UserResponse;
import com.example.ehe_server.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSearchServiceInterface {
    PaginatedResponse<UserResponse> searchUsers(Integer userId,
                                                String username,
                                                String email,
                                                User.AccountStatus accountStatus,
                                                LocalDateTime registrationDateToTime,
                                                LocalDateTime registrationDateFromTime,
                                                Integer size,
                                                Integer pag);
}