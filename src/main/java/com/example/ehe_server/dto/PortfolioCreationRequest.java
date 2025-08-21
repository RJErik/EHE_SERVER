package com.example.ehe_server.dto;

public class PortfolioCreationRequest {
    private String portfolioName;
    private Integer apiKeyId;

    // Getters and setters
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
