package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserSearchResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSearchServiceInterface {
    List<UserSearchResponse> searchUsers(Integer userId,
                                         String username,
                                         String email,
                                         String accountStatus,
                                         LocalDateTime registrationDateToTime,
                                         LocalDateTime registrationDateFromTime);
}