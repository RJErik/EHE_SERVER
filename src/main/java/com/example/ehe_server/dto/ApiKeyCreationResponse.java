package com.example.ehe_server.dto;

import java.util.Objects;

public class ApiKeyCreationResponse {
    private String message;
    private Long apiKeyId;

    public ApiKeyCreationResponse() {}

    public ApiKeyCreationResponse(String message, Long apiKeyId) {
        this.message = message;
        this.apiKeyId = apiKeyId;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Long getApiKeyId() {
        return apiKeyId;
    }
    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyCreationResponse that = (ApiKeyCreationResponse) o;
        return Objects.equals(message, that.message) &&
                Objects.equals(apiKeyId, that.apiKeyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, apiKeyId);
    }
}