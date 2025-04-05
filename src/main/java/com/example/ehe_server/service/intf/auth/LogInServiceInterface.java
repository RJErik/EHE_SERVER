package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface LogInServiceInterface {
    Map<String, Object> authenticateUser(LoginRequest request, HttpServletResponse response);
    void logoutUser(HttpServletResponse response);
}
