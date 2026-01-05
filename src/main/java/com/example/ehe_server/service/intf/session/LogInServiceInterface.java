package com.example.ehe_server.service.intf.session;

import jakarta.servlet.http.HttpServletResponse;

public interface LogInServiceInterface {
    void authenticateUser(String email, String password, HttpServletResponse response);
}
