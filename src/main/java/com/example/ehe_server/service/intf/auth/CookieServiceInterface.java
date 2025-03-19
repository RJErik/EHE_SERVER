package com.example.ehe_server.service.intf.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface CookieServiceInterface {
    void createJwtCookie(String jwtToken, HttpServletResponse response);
    void clearJwtCookie(HttpServletResponse response);
}
