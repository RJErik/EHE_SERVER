package com.example.ehe_server.service.intf.auth;

import java.util.List;

public interface JwtTokenGeneratorInterface {
    String generateToken(Long userId, String role);
}
