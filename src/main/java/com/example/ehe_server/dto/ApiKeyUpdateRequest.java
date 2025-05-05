package com.example.ehe_server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ApiKeyUpdateRequest {
    @NotNull(message = "API key ID is required")
    private Integer apiKeyId;

    @NotBlank(message = "Platform name is required")
    private String platformName;

    @NotBlank(message = "API key value is required")
    private String apiKeyValue;

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

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }
}
