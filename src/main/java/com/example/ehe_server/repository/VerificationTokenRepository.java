package com.example.ehe_server.repository;

import com.example.ehe_server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    // Find a token by its string value (used for verification)
    Optional<VerificationToken> findByTokenHash(String token);

    List<VerificationToken> findByUser_UserIdAndTokenTypeAndStatus(Integer user_userId, VerificationToken.TokenType tokenType, VerificationToken.TokenStatus status);

    // Count tokens issued recently for a user/type (used for rate limiting)
    int countByUser_UserIdAndTokenTypeAndIssueDateAfter(Integer userId, VerificationToken.TokenType type, LocalDateTime timestamp);

    List<VerificationToken> findByTokenTypeAndStatus(VerificationToken.TokenType tokenType, VerificationToken.TokenStatus status);
}