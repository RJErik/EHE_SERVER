package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.JwtClaimServiceInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPublicKey;

@Service
@Transactional
public class JwtClaimService implements JwtClaimServiceInterface {

    private final RSAPublicKey publicKey;

    public JwtClaimService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("user_id", Integer.class);
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
            // Now get it with proper type casting
            return claims.get("role", String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
