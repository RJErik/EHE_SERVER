package ehe_server.repository;

import ehe_server.entity.JwtRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken, Integer> {

    List<JwtRefreshToken> findByJwtRefreshTokenExpiryDateBefore(LocalDateTime date);

    Optional<JwtRefreshToken> findFirstByJwtRefreshTokenExpiryDateAfterOrderByJwtRefreshTokenExpiryDateAsc(LocalDateTime date);

    List<JwtRefreshToken> findByUser_UserId(Integer userId);

    Optional<JwtRefreshToken> findByJwtRefreshTokenHash(String hash);
}