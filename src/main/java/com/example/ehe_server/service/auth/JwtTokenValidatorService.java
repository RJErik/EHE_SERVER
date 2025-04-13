package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Debug - print all claims to see what's actually in the token
            System.out.println("DEBUG - JWT Claims: " + claims.toString());

            // Try direct access without type specification first
            Object roleObj = claims.get("role");
            System.out.println("DEBUG - Role object type: " + (roleObj != null ? roleObj.getClass().getName() : "null"));
            System.out.println("DEBUG - Role object value: " + roleObj);

            // Now get it with proper type casting
            String role = claims.get("role", String.class);
            System.out.println("DEBUG - Extracted role string: '" + role + "'");

            return role;
        } catch (Exception e) {
            System.out.println("DEBUG - Exception extracting role: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
