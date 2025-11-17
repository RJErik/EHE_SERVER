package com.example.ehe_server.dto;

import jakarta.validation.constraints.NotNull;

public class ApiKeyRemoveRequest {
    private Integer apiKeyId;

    // Getter and setter
    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
