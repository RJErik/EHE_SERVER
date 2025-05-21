// AutomatedTradeRuleSearchRequest.java
package com.example.ehe_server.dto;

import java.math.BigDecimal;

public class AutomatedTradeRuleSearchRequest {
    private Integer portfolioId;
    private String platform;
    private String symbol;
    private String conditionType;
    private String actionType;
    private String quantityType;
    private BigDecimal minThresholdValue;
    private BigDecimal maxThresholdValue;

    // Getters and setters
    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(String quantityType) {
        this.quantityType = quantityType;
    }

    public BigDecimal getMinThresholdValue() {
        return minThresholdValue;
    }

    public void setMinThresholdValue(BigDecimal minThresholdValue) {
        this.minThresholdValue = minThresholdValue;
    }

    public BigDecimal getMaxThresholdValue() {
        return maxThresholdValue;
    }

    public void setMaxThresholdValue(BigDecimal maxThresholdValue) {
        this.maxThresholdValue = maxThresholdValue;
    }
}