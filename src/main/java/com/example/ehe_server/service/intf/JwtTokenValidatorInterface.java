package com.example.ehe_server.service.intf;

public interface JwtTokenValidatorInterface {
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
}
