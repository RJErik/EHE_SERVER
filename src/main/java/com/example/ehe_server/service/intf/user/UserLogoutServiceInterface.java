package com.example.ehe_server.service.intf.user;

import jakarta.servlet.http.HttpServletResponse;

public interface UserLogoutServiceInterface {
    void logoutUser(Integer userId, HttpServletResponse response);
}
