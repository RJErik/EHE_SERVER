package com.example.ehe_server.service.intf.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface JwtTokenRenewalServiceInterface {
    /**
     * Renews the JWT token for the currently authenticated user
     *
     * @param userId The ID of the user requesting token renewal
     * @param response HTTP response to set the new cookie
     * @return A map containing success status and messages
     */
    void renewToken(Integer userId, HttpServletRequest request, HttpServletResponse response);
}
