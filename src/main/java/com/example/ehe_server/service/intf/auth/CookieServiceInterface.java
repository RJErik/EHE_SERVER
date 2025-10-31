package com.example.ehe_server.service.intf.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface CookieServiceInterface {
    void addJwtAccessCookie(String jwtToken, HttpServletResponse response);
    void addJwtRefreshCookie(String jwtToken, HttpServletResponse response);
    void clearJwtCookies(HttpServletResponse response);
}
