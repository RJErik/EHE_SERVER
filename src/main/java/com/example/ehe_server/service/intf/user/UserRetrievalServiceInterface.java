package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.UserRetrievalResponse;

import java.util.List;

public interface UserRetrievalServiceInterface {
    public List<UserRetrievalResponse> getUsers();
}
