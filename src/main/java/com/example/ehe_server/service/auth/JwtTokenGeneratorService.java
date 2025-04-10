package com.example.ehe_server.service.auth;

import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenGeneratorService implements JwtTokenGeneratorInterface {

    private final RSAPrivateKey privateKey;

    @Value("${jwt.expiration.time}")
    private long jwtExpirationTime;

    public JwtTokenGeneratorService(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public String generateToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationTime);

        return Jwts.builder()
                .claim("user_id", userId)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
