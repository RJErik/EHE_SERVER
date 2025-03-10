package com.example.ehe_server.service.security;

import com.example.ehe_server.service.intf.security.JwtTokenValidatorInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;

@Service
public class JwtTokenValidatorService implements JwtTokenValidatorInterface {

    private final RSAPublicKey publicKey;

    public JwtTokenValidatorService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("user_id", Long.class);
    }
}
