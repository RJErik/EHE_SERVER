package com.example.ehe_server.service.intf.auth;

public interface JwtClaimServiceInterface {
    Integer getUserIdFromToken(String token);
    String getRoleFromToken(String token); // Changed from List<String> getRolesFromToken
}
