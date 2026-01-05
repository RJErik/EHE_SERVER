package com.example.ehe_server.dto;

import com.example.ehe_server.entity.Alert;

public class AlertSearchRequest {
    private String platform;
    private String symbol;
    private Alert.ConditionType conditionType;

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

    public Alert.ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(Alert.ConditionType conditionType) {
        this.conditionType = conditionType;
    }
}
