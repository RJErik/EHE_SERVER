package com.example.ehe_server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_key_id")
    private Integer apiKeyId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @Column(name = "api_key_value", nullable = false)
    @Convert(converter = ColumnEncryptor.class)
    private String apiKeyValue;

    @Column(name = "secret_key", nullable = false)
    @Convert(converter = ColumnEncryptor.class)
    private String secretKey;

    @CreationTimestamp
    @Column(name = "date_added", nullable = false, updatable = false, insertable = false)
    private LocalDateTime dateAdded;

    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValueEncrypt) {
        this.apiKeyValue = apiKeyValueEncrypt;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKeyEncrypt) {
        this.secretKey = secretKeyEncrypt;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }
}
