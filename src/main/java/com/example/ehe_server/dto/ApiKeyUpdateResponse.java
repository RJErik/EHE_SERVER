package com.example.ehe_server.dto;

import java.util.Objects;

public class ApiKeyUpdateResponse {
    private Integer apiKeyId;
    private String platformName;

    public ApiKeyUpdateResponse() {
    }

    public ApiKeyUpdateResponse(Integer apiKeyId, String platformName) {
        this.apiKeyId = apiKeyId;
        this.platformName = platformName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyUpdateResponse that = (ApiKeyUpdateResponse) o;
        return Objects.equals(apiKeyId, that.apiKeyId) &&
                Objects.equals(platformName, that.platformName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiKeyId, platformName);
    }
}