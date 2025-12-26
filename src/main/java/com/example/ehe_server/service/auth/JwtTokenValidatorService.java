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
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException e) {
            // Invalid or expired token
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
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
            // No matching hash found
            return false;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException e) {
            // Invalid or expired token
            return false;
        }
    }
}
