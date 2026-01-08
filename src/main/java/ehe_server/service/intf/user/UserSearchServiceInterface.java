package ehe_server.service.intf.user;

import ehe_server.dto.PaginatedResponse;
import ehe_server.dto.UserResponse;
import ehe_server.entity.User;

import java.time.LocalDateTime;

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