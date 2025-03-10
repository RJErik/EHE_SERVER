package com.example.ehe_server.service.intf.auth;

import com.example.ehe_server.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthenticationServiceInterface {
    /**
     * Authenticate a user based on login credentials
     * @param request The login request containing credentials
     * @param response HttpServletResponse to add cookies to
     * @return A map containing authentication results and user information
     */
    Map<String, Object> authenticateUser(LoginRequest request, HttpServletResponse response);

    /**
     * Log out a user by clearing authentication cookies
     * @param response HttpServletResponse to clear cookies
     */
    void logoutUser(HttpServletResponse response);
}
