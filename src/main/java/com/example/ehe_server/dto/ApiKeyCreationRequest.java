package com.example.ehe_server.dto;

import com.example.ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.exception.custom.MissingApiKeyValueException;
import com.example.ehe_server.exception.custom.MissingPlatformNameException;
import com.example.ehe_server.exception.custom.MissingSecretKeyException;

public class ApiKeyCreationRequest {
    @NotEmptyString(exception = MissingPlatformNameException.class)
    private String platformName;

    @NotEmptyString(exception = MissingApiKeyValueException.class)
    private String apiKeyValue;

    @NotEmptyString(exception = MissingSecretKeyException.class)
    private String secretKey;

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
