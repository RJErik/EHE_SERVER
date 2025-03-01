package com.example.ehe_server.DatabaseEntities;

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

    // Other getters and setters following the pattern above
    // ...
}

