package com.example.ehe_server.entity;

import jakarta.persistence.*;
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

    @Column(name = "platform_name", nullable = false, length = 100)
    private String platformName;

    @Column(name = "api_key_value_encrypt", nullable = false)
    private String apiKeyValueEncrypt;

    @Column(name = "secret_key_encrypt")
    private String secretKeyEncrypt;

    @Column(name = "date_added", nullable = false,
            updatable = false, insertable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateAdded;

    // Getters and setters
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

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getApiKeyValueEncrypt() {
        return apiKeyValueEncrypt;
    }

    public void setApiKeyValueEncrypt(String apiKeyValueEncrypt) {
        this.apiKeyValueEncrypt = apiKeyValueEncrypt;
    }

    public String getSecretKeyEncrypt() {
        return secretKeyEncrypt;
    }

    public void setSecretKeyEncrypt(String secretKeyEncrypt) {
        this.secretKeyEncrypt = secretKeyEncrypt;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }
}
