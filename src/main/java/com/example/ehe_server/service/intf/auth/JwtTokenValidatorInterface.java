package com.example.ehe_server.service.intf.auth;

public interface JwtTokenValidatorInterface {
    boolean validateAccessToken(String token);
    boolean validateRefreshToken(String token);
    Integer getUserIdFromToken(String token);
    String getRoleFromToken(String token); // Changed from List<String> getRolesFromToken
}
