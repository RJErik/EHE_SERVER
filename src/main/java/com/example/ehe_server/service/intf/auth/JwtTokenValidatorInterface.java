package com.example.ehe_server.service.intf.auth;

public interface JwtTokenValidatorInterface {
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
    String getRoleFromToken(String token); // Changed from List<String> getRolesFromToken
}
