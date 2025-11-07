package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserUpdateResponse;
import com.example.ehe_server.entity.User;

import java.time.LocalDateTime;

public interface UserUpdateServiceInterface {
    UserUpdateResponse updateUserInfo(Integer userId, String username, String email, String password, String accountStatus);
}
