package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.PaginatedResponse;
import com.example.ehe_server.dto.UserResponse;

import java.util.List;

public interface UserRetrievalServiceInterface {
    PaginatedResponse<UserResponse> getUsers(Integer size, Integer page);
}
