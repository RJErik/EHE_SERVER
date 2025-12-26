package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.JwtClaimServiceInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

@Service
public class JwtClaimService implements JwtClaimServiceInterface {

    private final RSAPublicKey publicKey;

    public JwtClaimService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public static class TokenDetails {
        private final Integer userId;
        private final String role;

        public TokenDetails(Integer userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public Integer getUserId() { return userId; }
        public String getRole() { return role; }
    }

    public TokenDetails parseTokenDetails(String token) {
        // Fail Fast
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        // Expensive Crypto Math happens ONLY here
        Claims claims = extractClaims(token);

        if (claims != null) {
            return new TokenDetails(
                    claims.get("user_id", Integer.class),
                    claims.get("role", String.class)
            );
        }
        return null;
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
            return null;
        }
    }
}
