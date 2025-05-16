package com.example.ehe_server.dto;

public class ApiKeyDto {
    private Integer apiKeyId;
    private String platformName;
    private String maskedApiKeyValue;
    private String maskedSecretKey;

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
}
