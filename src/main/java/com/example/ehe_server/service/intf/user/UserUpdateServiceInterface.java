package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserResponse;
import com.example.ehe_server.entity.User;

public interface UserUpdateServiceInterface {
    UserResponse updateUserInfo(Integer userId, String username, String email, String password, User.AccountStatus accountStatus);
}
