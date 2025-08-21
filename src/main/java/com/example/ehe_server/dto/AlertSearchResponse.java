package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects; // Required for Objects.equals and Objects.hash

public class AlertSearchResponse {
    private Integer id;
    private String platform;
    private String symbol;
    private String conditionType;
    private BigDecimal thresholdValue;
    private String dateCreated;
    private boolean isActive;

    // No-argument constructor (equivalent to @NoArgsConstructor)
    public AlertSearchResponse() {
    }

    // All-argument constructor (equivalent to @AllArgsConstructor)
    public AlertSearchResponse(Integer id, String platform, String symbol, String conditionType, BigDecimal thresholdValue, String dateCreated, boolean isActive) {
        this.id = id;
        this.platform = platform;
        this.symbol = symbol;
        this.conditionType = conditionType;
        this.thresholdValue = thresholdValue;
        this.dateCreated = dateCreated;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
