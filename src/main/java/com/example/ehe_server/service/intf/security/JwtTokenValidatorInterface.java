package com.example.ehe_server.service.intf.security;

import java.util.List;

public interface JwtTokenValidatorInterface {
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
    List<String> getRolesFromToken(String token);
}
