package com.example.ehe_server.service.intf.security;

public interface JwtTokenGeneratorInterface {
    String generateToken(Long userId);
}