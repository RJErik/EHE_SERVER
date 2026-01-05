package com.example.ehe_server.dto;

import com.example.ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.annotation.validation.NotNullField;
import com.example.ehe_server.annotation.validation.PositiveAmount;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.*;

import java.math.BigDecimal;

public class AlertCreationRequest {
    @NotEmptyString(exception = MissingPlatformNameException.class)
    private String platform;
    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String symbol;
    @NotNullField(exception = MissingConditionTypeException.class)
    private Alert.ConditionType conditionType;
    @NotNullField(exception = MissingThresholdValueException.class)
    @PositiveAmount(exception = InvalidThresholdValueException.class)
    private BigDecimal thresholdValue;

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

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }
}
