package com.example.ehe_server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "automated_trade_rule")
public class AutomatedTradeRule {

    public enum ConditionType {
        @Column(name = "Price above") PRICE_ABOVE,
        @Column(name = "Price below") PRICE_BELOW
    }

    public enum ActionType {
        @Column(name = "Buy") BUY,
        @Column(name = "Sell") SELL
    }

    public enum QuantityType {
        @Column(name = "Quantity") QUANTITY,
        @Column(name = "Quote Order Quantity") QUOTE_ORDER_QTY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "automated_trade_rule_id")
    private Integer automatedTradeRuleId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "platform_stock_id", nullable = false)
    private PlatformStock platformStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_type", nullable = false)
    private QuantityType quantityType;

    @DecimalMin(value = "0.00000001", inclusive = false)
    @Digits(integer = 10, fraction = 8)
    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @DecimalMin(value = "0.00000001", inclusive = false)
    @Digits(integer = 10, fraction = 8)
    @Column(name = "threshold_value", nullable = false, precision = 18, scale = 8)
    private BigDecimal thresholdValue;

    @ManyToOne
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @Column(name = "date_created", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateCreated;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // Getters and setters
    public Integer getAutomatedTradeRuleId() {
        return automatedTradeRuleId;
    }

    public void setAutomatedTradeRuleId(Integer automatedTradeRuleId) {
        this.automatedTradeRuleId = automatedTradeRuleId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public PlatformStock getPlatformStock() {
        return platformStock;
    }

    public void setPlatformStock(PlatformStock platformStock) {
        this.platformStock = platformStock;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public QuantityType getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(QuantityType quantityType) {
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

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}