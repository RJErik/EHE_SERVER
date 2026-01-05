package com.example.ehe_server.dto;

import com.example.ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.annotation.validation.NotNullField;
import com.example.ehe_server.annotation.validation.RegexPattern;
import com.example.ehe_server.exception.custom.InvalidPortfolioName;
import com.example.ehe_server.exception.custom.MissingApiKeyIdException;
import com.example.ehe_server.exception.custom.MissingPortfolioNameException;

public class PortfolioCreationRequest {
    @NotEmptyString(exception = MissingPortfolioNameException.class)
    @RegexPattern(pattern = "^[a-zA-Z0-9_]{1,100}$",
    exception = InvalidPortfolioName.class)
    private String portfolioName;
    @NotNullField(exception = MissingApiKeyIdException.class)
    private Integer apiKeyId;

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
