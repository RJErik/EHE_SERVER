package com.example.ehe_server.service.intf.user;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public interface JwtTokenRenewalServiceInterface {
    /**
     * Renews the JWT token for the currently authenticated user
     *
     * @param userId The ID of the user requesting token renewal
     * @param response HTTP response to set the new cookie
     * @return A map containing success status and messages
     */
    Map<String, Object> renewToken(Long userId, HttpServletResponse response);
}
