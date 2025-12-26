package com.example.ehe_server.repository;

import com.example.ehe_server.entity.JwtRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken, Integer> {

    /**
     * Find all tokens that have expired before the given date
     */
    List<JwtRefreshToken> findByJwtRefreshTokenExpiryDateBefore(LocalDateTime date);

    /**
     * Find the first token that expires after the given date, ordered by expiry date
     */
    Optional<JwtRefreshToken> findFirstByJwtRefreshTokenExpiryDateAfterOrderByJwtRefreshTokenExpiryDateAsc(LocalDateTime date);

    /**
     * Find all tokens for a specific user
     */
    List<JwtRefreshToken> findByUser_UserId(Integer userId);
}