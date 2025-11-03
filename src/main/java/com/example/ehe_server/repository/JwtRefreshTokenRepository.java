package com.example.ehe_server.repository;

import com.example.ehe_server.entity.JwtRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken, Integer> {
}
