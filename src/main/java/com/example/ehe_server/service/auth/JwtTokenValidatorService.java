package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.service.intf.auth.JwtTokenValidatorInterface;
import com.example.ehe_server.service.intf.token.TokenHashServiceInterface;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

@Service
@Transactional
public class JwtTokenValidatorService implements JwtTokenValidatorInterface {

    private final RSAPublicKey publicKey;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final TokenHashServiceInterface tokenHashService;

    public JwtTokenValidatorService(RSAPublicKey publicKey,
                                    JwtRefreshTokenRepository jwtRefreshTokenRepository,
                                    TokenHashServiceInterface tokenHashService) {
        this.publicKey = publicKey;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.tokenHashService = tokenHashService;
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
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String hashedToken = tokenHashService.hashToken(token);

            Optional<JwtRefreshToken> storedToken = jwtRefreshTokenRepository.findByJwtRefreshTokenHash(hashedToken);

            return storedToken.isPresent();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
