package com.example.ehe_server.dto;

import jakarta.validation.constraints.NotNull;

public class ApiKeyRemoveRequest {
    @NotNull(message = "API key ID is required")
    private Integer apiKeyId;

    // Getter and setter
    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
