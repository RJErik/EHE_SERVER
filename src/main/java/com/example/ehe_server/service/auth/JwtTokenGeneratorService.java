package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

@Service
public class JwtTokenGeneratorService implements JwtTokenGeneratorInterface {

    private final RSAPrivateKey privateKey;

    @Value("${jwt.access.expiration.time}")
    private long jwtAccessExpirationTime;

    @Value("${jwt.refresh.expiration.time}")
    private long jwtRefreshExpirationTime;

    public JwtTokenGeneratorService(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessExpirationTime);

        return Jwts.builder()
                .claim("user_id", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationTime);

        return Jwts.builder()
                .claim("user_id", userId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
