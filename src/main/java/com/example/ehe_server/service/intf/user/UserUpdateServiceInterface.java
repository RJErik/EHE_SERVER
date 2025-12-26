package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserResponse;

public interface UserUpdateServiceInterface {
    UserResponse updateUserInfo(Integer userId, String username, String email, String password, String accountStatus);
}
