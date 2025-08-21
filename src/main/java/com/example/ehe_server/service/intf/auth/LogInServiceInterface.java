package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface LogInServiceInterface {
    void authenticateUser(LoginRequest request, HttpServletResponse response);
    // logoutUser method removed
}
