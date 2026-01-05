package com.example.ehe_server.service.auth;

import com.example.ehe_server.exception.custom.MissingRoleException;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.properties.JwtProperties;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

@Service
@Transactional
public class JwtTokenGeneratorService implements JwtTokenGeneratorInterface {

    private final RSAPrivateKey privateKey;
    private final JwtProperties jwtConfig;

    public JwtTokenGeneratorService(RSAPrivateKey privateKey, JwtProperties jwtConfig) {
        this.privateKey = privateKey;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public String generateAccessToken(Integer userId, String role) {
        return buildToken(userId, role, jwtConfig.getJwtAccessExpirationTime());
    }

    @Override
    public String generateRefreshToken(Integer userId, String role) {
        return buildToken(userId, role, jwtConfig.getJwtRefreshExpirationTime());
    }

    private String buildToken(Integer userId, String role, long expirationMillis) {

        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (role == null || role.trim().isEmpty()) {
            throw new MissingRoleException();
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("user_id", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}