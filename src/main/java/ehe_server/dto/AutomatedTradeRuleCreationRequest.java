// AutomatedTradeRuleCreationRequest.java
package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.annotation.validation.PositiveAmount;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.exception.custom.*;

import java.math.BigDecimal;

public class AutomatedTradeRuleCreationRequest {
    @NotNullField(exception = MissingPortfolioIdException.class)
    private Integer portfolioId;
    @NotEmptyString(exception = MissingPortfolioNameException.class)
    private String platform;
    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String symbol;
    @NotNullField(exception = MissingConditionTypeException.class)
    private AutomatedTradeRule.ConditionType conditionType;
    @NotNullField(exception = MissingActionTypeException.class)
    private AutomatedTradeRule.ActionType actionType;
    @NotNullField(exception = MissingQuantityTypeException.class)
    private AutomatedTradeRule.QuantityType quantityType;
    @NotNullField(exception = MissingQuantityException.class)
    @PositiveAmount(exception = InvalidQuantityException.class)
    private BigDecimal quantity;
    @NotNullField(exception = MissingThresholdValueException.class)
    @PositiveAmount(exception = InvalidThresholdValueException.class)
    private BigDecimal thresholdValue;

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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }
}