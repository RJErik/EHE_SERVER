package com.example.ehe_server.service.intf.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface CookieServiceInterface {
    /**
     * Create and add a JWT cookie to the HTTP response
     * @param jwtToken The JWT token to store in the cookie
     * @param response The HTTP response to add the cookie to
     */
    void createJwtCookie(String jwtToken, HttpServletResponse response);

    /**
     * Clear the JWT cookie from the HTTP response
     * @param response The HTTP response to add the cleared cookie to
     */
    void clearJwtCookie(HttpServletResponse response);
}
