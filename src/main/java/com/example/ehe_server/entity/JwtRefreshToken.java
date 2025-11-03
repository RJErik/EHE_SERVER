package com.example.ehe_server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jwt_refresh_token")
public class JwtRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "jwt_refresh_token_id")
    private Integer jwtRefreshTokenId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "jwt_refresh_token_hash", nullable = false, length = 255)
    private String jwtRefreshTokenHash;

    @Column(name = "jwt_refresh_token_expiry_date", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '7 day'")
    private LocalDateTime jwtRefreshTokenExpiryDate;

    @Column(name = "jwt_refresh_token_max_expiry_date", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '30 day'")
    private LocalDateTime jwtRefreshTokenMaxExpiryDate;

    // Getters and setters
    public Integer getJwtRefreshTokenId() {
        return jwtRefreshTokenId;
    }

    public void setJwtRefreshTokenId(Integer jwtRefreshTokenId) {
        this.jwtRefreshTokenId = jwtRefreshTokenId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getJwtRefreshTokenHash() {
        return jwtRefreshTokenHash;
    }

    public void setJwtRefreshTokenHash(String jwtRefreshTokenHash) {
        this.jwtRefreshTokenHash = jwtRefreshTokenHash;
    }

    public LocalDateTime getJwtRefreshTokenExpiryDate() {
        return jwtRefreshTokenExpiryDate;
    }

    public void setJwtRefreshTokenExpiryDate(LocalDateTime jwtRefreshTokenExpiryDate) {
        this.jwtRefreshTokenExpiryDate = jwtRefreshTokenExpiryDate;
    }

    public LocalDateTime getJwtRefreshTokenMaxExpiryDate() {
        return jwtRefreshTokenMaxExpiryDate;
    }

    public void setJwtRefreshTokenMaxExpiryDate(LocalDateTime jwtRefreshTokenMaxExpiryDate) {
        this.jwtRefreshTokenMaxExpiryDate = jwtRefreshTokenMaxExpiryDate;
    }
}