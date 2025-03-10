package com.example.ehe_server.service.intf.security;

public interface JwtTokenValidatorInterface {
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
}
