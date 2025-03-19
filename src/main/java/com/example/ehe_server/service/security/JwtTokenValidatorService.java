package com.example.ehe_server.service.security;

import com.example.ehe_server.service.intf.security.JwtTokenValidatorInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

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
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException e) {
            // Invalid or expired token
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("user_id", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("roles", ArrayList.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
