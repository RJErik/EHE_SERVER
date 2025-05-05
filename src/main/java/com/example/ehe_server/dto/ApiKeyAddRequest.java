package com.example.ehe_server.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyAddRequest {
    @NotBlank(message = "Platform name is required")
    private String platformName;

    @NotBlank(message = "API key value is required")
    private String apiKeyValue;

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
}
