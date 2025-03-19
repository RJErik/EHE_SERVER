package com.example.ehe_server.service.intf.security;

import java.util.List;

public interface JwtTokenGeneratorInterface {
    String generateToken(Long userId, List<String> roles);
}
