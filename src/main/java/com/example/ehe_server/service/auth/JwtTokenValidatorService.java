package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Service
public class JwtTokenValidatorService implements JwtTokenValidatorInterface {

    private final RSAPublicKey publicKey;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;

    public JwtTokenValidatorService(RSAPublicKey publicKey,
                                    JwtRefreshTokenRepository jwtRefreshTokenRepository) {
        this.publicKey = publicKey;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
    }

    @Override
    public boolean validateAccessToken(String token) {
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
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extract user_id from the token claims
            Integer userId = claims.get("user_id", Integer.class);

            // Get only this user's refresh tokens from the database
            List<JwtRefreshToken> userRefreshTokens = jwtRefreshTokenRepository.findByUser_UserId(userId);

            // Check if the provided token matches any of this user's stored hashes
            for (JwtRefreshToken storedToken : userRefreshTokens) {
                if (BCrypt.checkpw(token, storedToken.getJwtRefreshTokenHash())) {
                    return true;
                }
            }
            System.out.println("No refresh token found");

            // No matching hash found
            return false;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException e) {
            // Invalid or expired token
            return false;
        } catch (Exception e) {
            return false;
        }
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
