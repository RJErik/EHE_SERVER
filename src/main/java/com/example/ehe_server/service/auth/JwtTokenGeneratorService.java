package com.example.ehe_server.service.auth;

import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

@Service
public class JwtTokenGeneratorService implements JwtTokenGeneratorInterface {

    private final RSAPrivateKey privateKey;
    private final JwtProperties jwtConfig;


    public JwtTokenGeneratorService(RSAPrivateKey privateKey, JwtProperties jwtConfig) {
        this.privateKey = privateKey;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public String generateAccessToken(Integer userId, String role) {
        if (userId == null || (role == null || role.trim().isEmpty())) {
            throw new IllegalArgumentException("Access token generation failed: userId and role cannot be null or empty.");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getJwtAccessExpirationTime());

        return Jwts.builder()
                .claim("user_id", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Integer userId, String role) {
        if (userId == null || (role == null || role.trim().isEmpty())) {
            throw new IllegalArgumentException("Refresh token generation failed: userId and role cannot be null or empty.");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getJwtRefreshExpirationTime());

        return Jwts.builder()
                .claim("user_id", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
