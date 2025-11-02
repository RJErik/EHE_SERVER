package com.example.ehe_server.service.intf.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface LogInServiceInterface {
    void authenticateUser(String email, String password, HttpServletResponse response);
    // logoutUser method removed
}
