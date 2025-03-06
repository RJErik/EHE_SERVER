package com.example.ehe_server.service.intf;

public interface JwtTokenGeneratorInterface {
    String generateToken(Long userId);
}