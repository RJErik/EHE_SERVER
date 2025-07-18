package com.example.ehe_server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_token")
public class VerificationToken {

    public enum TokenType {
        REGISTRATION, PASSWORD_RESET, EMAIL_CHANGE
    }

    public enum TokenStatus {
        ACTIVE,     // Token is valid and ready to be used
        USED,       // Token has been successfully used
        EXPIRED,    // Token passed its expiry date without being used
        INVALIDATED // Token was superseded by a newer token (e.g., resend)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_token_id")
    private Integer verificationTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 50)
    private TokenType tokenType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenStatus status = TokenStatus.ACTIVE; // Default status

    @Column(name = "issue_date", nullable = false, updatable = false)
    private LocalDateTime issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    // --- Constructors ---
    public VerificationToken() {
        this.issueDate = LocalDateTime.now(); // Default issue date
    }

    public VerificationToken(User user, String token, TokenType tokenType, LocalDateTime expiryDate) {
        this(); // Call default constructor
        this.user = user;
        this.token = token;
        this.tokenType = tokenType;
        this.expiryDate = expiryDate;
    }

    // --- Getters and Setters ---
    public Integer getVerificationTokenId() {
        return verificationTokenId;
    }

    public void setVerificationTokenId(Integer verificationTokenId) {
        this.verificationTokenId = verificationTokenId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    // --- Convenience method ---
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}