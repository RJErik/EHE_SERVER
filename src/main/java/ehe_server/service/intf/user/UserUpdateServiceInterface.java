package ehe_server.service.intf.user;

import ehe_server.dto.UserResponse;
import ehe_server.entity.User;

public interface UserUpdateServiceInterface {
    UserResponse updateUserInfo(Integer userId, String username, String email, String password, User.AccountStatus accountStatus);
}
