package com.example.ehe_server.service.intf.auth;

public interface JwtTokenGeneratorInterface {
    String generateAccessToken(Long userId, String role);
    String generateRefreshToken(Long userId, String role);
}
