package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class AutomatedTradeRuleRetrievalResponse {
    private Integer id;
    private Integer portfolioId;
    private String portfolioName;
    private String platform;
    private String symbol;
    private String conditionType;
    private String actionType;
    private String quantityType;
    private BigDecimal quantity;
    private BigDecimal thresholdValue;
    private String dateCreated;
    private boolean isActive;

    public AutomatedTradeRuleRetrievalResponse() {}

    public AutomatedTradeRuleRetrievalResponse(Integer id, Integer portfolioId, String portfolioName, String platform, String symbol, String conditionType, String actionType, String quantityType, BigDecimal quantity, BigDecimal thresholdValue, String dateCreated, boolean isActive) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.platform = platform;
        this.symbol = symbol;
        this.conditionType = conditionType;
        this.actionType = actionType;
        this.quantityType = quantityType;
        this.quantity = quantity;
        this.thresholdValue = thresholdValue;
        this.dateCreated = dateCreated;
        this.isActive = isActive;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getQuantityType() { return quantityType; }
    public void setQuantityType(String quantityType) { this.quantityType = quantityType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomatedTradeRuleRetrievalResponse that = (AutomatedTradeRuleRetrievalResponse) o;
        return isActive == that.isActive &&
                Objects.equals(id, that.id) &&
                Objects.equals(portfolioId, that.portfolioId) &&
                Objects.equals(portfolioName, that.portfolioName) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(conditionType, that.conditionType) &&
                Objects.equals(actionType, that.actionType) &&
                Objects.equals(quantityType, that.quantityType) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(thresholdValue, that.thresholdValue) &&
                Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, portfolioId, portfolioName, platform, symbol, conditionType, actionType, quantityType, quantity, thresholdValue, dateCreated, isActive);
    }
}