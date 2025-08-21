package com.example.ehe_server.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyCreationRequest {
    @NotBlank(message = "Platform name is required")
    private String platformName;

    @NotBlank(message = "API key value is required")
    private String apiKeyValue;

    @NotBlank(message = "Secret key value is required")
    private String secretKey;

    // Getters and setters
    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
