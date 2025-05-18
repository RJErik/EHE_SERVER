package com.example.ehe_server.dto;

import com.example.ehe_server.entity.Portfolio.PortfolioType;
import java.math.BigDecimal;

public class PortfolioSearchRequest {
    private PortfolioType type;
    private String platform;
    private BigDecimal minValue;
    private BigDecimal maxValue;

    // Getters and setters
    public PortfolioType getType() {
        return type;
    }

    public void setType(PortfolioType type) {
        this.type = type;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
}
