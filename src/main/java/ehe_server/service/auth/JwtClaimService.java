package ehe_server.service.auth;

import ehe_server.service.intf.auth.JwtClaimServiceInterface;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.SignatureException;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.RSAPublicKey;

@Service
@Transactional
public class JwtClaimService implements JwtClaimServiceInterface {

    private final RSAPublicKey publicKey;

    public JwtClaimService(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public record TokenDetails(Integer userId, String role) {
    }

    public TokenDetails parseTokenDetails(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

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
