package com.example.ehe_server.service.intf.auth;

public interface JwtTokenGeneratorInterface {
    String generateAccessToken(Integer userId, String role);
    String generateRefreshToken(Integer userId, String role);
}
