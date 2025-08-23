package com.example.ehe_server.dto;

import java.util.Objects;

public class ApiKeyUpdateResponse {
    private Integer apiKeyId;
    private String platformName;
    private String maskedApiKeyValue;
    private String maskedSecretKey;

    public ApiKeyUpdateResponse() {
    }

    public ApiKeyUpdateResponse(Integer apiKeyId, String platformName) {
        this.apiKeyId = apiKeyId;
        this.platformName = platformName;
    }

    public ApiKeyUpdateResponse(Integer apiKeyId, String platformName, String maskedApiKeyValue, String maskedSecretKey) {
        this.apiKeyId = apiKeyId;
        this.platformName = platformName;
        this.maskedApiKeyValue = maskedApiKeyValue;
        this.maskedSecretKey = maskedSecretKey;
    }

    // Getters and setters
    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getMaskedApiKeyValue() {
        return maskedApiKeyValue;
    }

    public void setMaskedApiKeyValue(String maskedApiKeyValue) {
        this.maskedApiKeyValue = maskedApiKeyValue;
    }

    public String getMaskedSecretKey() {
        return maskedSecretKey;
    }

    public void setMaskedSecretKey(String maskedSecretKey) {
        this.maskedSecretKey = maskedSecretKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyUpdateResponse that = (ApiKeyUpdateResponse) o;
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