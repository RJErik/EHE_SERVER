// AutomatedTradeRuleSearchRequest.java
package ehe_server.dto;

import ehe_server.entity.AutomatedTradeRule;

import java.math.BigDecimal;

public class AutomatedTradeRuleSearchRequest {
    private Integer portfolioId;
    private String platform;
    private String symbol;
    private AutomatedTradeRule.ConditionType conditionType;
    private AutomatedTradeRule.ActionType actionType;
    private AutomatedTradeRule.QuantityType quantityType;
    private BigDecimal minThresholdValue;
    private BigDecimal maxThresholdValue;

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

    public AutomatedTradeRule.ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(AutomatedTradeRule.ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public AutomatedTradeRule.ActionType getActionType() {
        return actionType;
    }

    public void setActionType(AutomatedTradeRule.ActionType actionType) {
        this.actionType = actionType;
    }

    public AutomatedTradeRule.QuantityType getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(AutomatedTradeRule.QuantityType quantityType) {
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