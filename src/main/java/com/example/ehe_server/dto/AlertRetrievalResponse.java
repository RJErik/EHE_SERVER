package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class AlertRetrievalResponse {
    private Integer id;
    private String platform;
    private String symbol;
    private String conditionType;
    private BigDecimal thresholdValue;
    private String dateCreated;
    private boolean isActive;

    public AlertRetrievalResponse() {}

    public AlertRetrievalResponse(Integer id, String platform, String symbol, String conditionType, BigDecimal thresholdValue, String dateCreated, boolean isActive) {
        this.id = id;
        this.platform = platform;
        this.symbol = symbol;
        this.conditionType = conditionType;
        this.thresholdValue = thresholdValue;
        this.dateCreated = dateCreated;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }
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
        AlertRetrievalResponse that = (AlertRetrievalResponse) o;
        return isActive == that.isActive &&
                Objects.equals(id, that.id) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(conditionType, that.conditionType) &&
                Objects.equals(thresholdValue, that.thresholdValue) &&
                Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform, symbol, conditionType, thresholdValue, dateCreated, isActive);
    }
}