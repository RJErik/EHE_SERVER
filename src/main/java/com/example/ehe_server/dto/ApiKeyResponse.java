package com.example.ehe_server.dto;

import java.util.Objects;

public class ApiKeyResponse {
    private Integer apiKeyId;
    private String platformName;
    private String maskedApiKeyValue;
    private String maskedSecretKey;

    // No-argument constructor
    public ApiKeyResponse() {}

    // Constructor with apiKeyId only
    public ApiKeyResponse(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    // Constructor with apiKeyId and platformName
    public ApiKeyResponse(Integer apiKeyId, String platformName) {
        this.apiKeyId = apiKeyId;
        this.platformName = platformName;
    }

    // All-argument constructor
    public ApiKeyResponse(Integer apiKeyId, String platformName, String maskedApiKeyValue, String maskedSecretKey) {
        this.apiKeyId = apiKeyId;
        this.platformName = platformName;
        this.maskedApiKeyValue = maskedApiKeyValue;
        this.maskedSecretKey = maskedSecretKey;
    }

    // Getters and Setters
    public Integer getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Integer apiKeyId) { this.apiKeyId = apiKeyId; }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }

    public String getMaskedApiKeyValue() { return maskedApiKeyValue; }
    public void setMaskedApiKeyValue(String maskedApiKeyValue) { this.maskedApiKeyValue = maskedApiKeyValue; }

    public String getMaskedSecretKey() { return maskedSecretKey; }
    public void setMaskedSecretKey(String maskedSecretKey) { this.maskedSecretKey = maskedSecretKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyResponse that = (ApiKeyResponse) o;
        return Objects.equals(apiKeyId, that.apiKeyId) &&
                Objects.equals(platformName, that.platformName) &&
                Objects.equals(maskedApiKeyValue, that.maskedApiKeyValue) &&
                Objects.equals(maskedSecretKey, that.maskedSecretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKeyId, platformName, maskedApiKeyValue, maskedSecretKey);
    }
}