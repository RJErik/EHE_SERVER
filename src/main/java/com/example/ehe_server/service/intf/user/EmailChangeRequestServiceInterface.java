package com.example.ehe_server.service.intf.user;

public interface EmailChangeRequestServiceInterface {
    void requestEmailChange(Integer userId, String newEmail);
}
