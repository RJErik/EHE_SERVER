package com.example.ehe_server.repository;

import com.example.ehe_server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    Optional<VerificationToken> findByTokenHash(String token);

    List<VerificationToken> findByUser_UserIdAndTokenTypeAndStatus(Integer user_userId, VerificationToken.TokenType tokenType, VerificationToken.TokenStatus status);

    int countByUser_UserIdAndTokenTypeAndIssueDateAfter(Integer userId, VerificationToken.TokenType type, LocalDateTime timestamp);
}