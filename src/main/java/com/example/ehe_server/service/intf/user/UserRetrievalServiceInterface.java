package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserResponse;

import java.util.List;

public interface UserRetrievalServiceInterface {
    public List<UserResponse> getUsers();
}
