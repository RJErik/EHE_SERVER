package com.example.ehe_server.dto;

import com.example.ehe_server.entity.AutomatedTradeRule;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class AutomatedTradeRuleResponse {
    private Integer id;
    private Integer portfolioId;
    private String portfolioName;
    private String platform;
    private String symbol;
    private AutomatedTradeRule.ConditionType conditionType;
    private AutomatedTradeRule.ActionType actionType;
    private AutomatedTradeRule.QuantityType quantityType;
    private BigDecimal quantity;
    private BigDecimal thresholdValue;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreated;

    public AutomatedTradeRuleResponse() {}

    public AutomatedTradeRuleResponse(Integer id, Integer portfolioId, String portfolioName, String platform, String symbol, AutomatedTradeRule.ConditionType conditionType, AutomatedTradeRule.ActionType actionType, AutomatedTradeRule.QuantityType quantityType, BigDecimal quantity, BigDecimal thresholdValue, LocalDateTime dateCreated) {
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

    public AutomatedTradeRule.ConditionType getConditionType() { return conditionType; }
    public void setConditionType(AutomatedTradeRule.ConditionType conditionType) { this.conditionType = conditionType; }

    public AutomatedTradeRule.ActionType getActionType() { return actionType; }
    public void setActionType(AutomatedTradeRule.ActionType actionType) { this.actionType = actionType; }

    public AutomatedTradeRule.QuantityType getQuantityType() { return quantityType; }
    public void setQuantityType(AutomatedTradeRule.QuantityType quantityType) { this.quantityType = quantityType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomatedTradeRuleResponse that = (AutomatedTradeRuleResponse) o;
        return Objects.equals(id, that.id) &&
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
        return Objects.hash(id, portfolioId, portfolioName, platform, symbol, conditionType, actionType, quantityType, quantity, thresholdValue, dateCreated);
    }
}