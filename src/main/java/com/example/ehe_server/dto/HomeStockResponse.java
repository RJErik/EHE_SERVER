package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class HomeStockResponse {
    private String platform;
    private String symbol;
    private BigDecimal changePercentage;

    // No-argument constructor
    public HomeStockResponse() {}

    // All-argument constructor
    public HomeStockResponse(String platform, String symbol, BigDecimal changePercentage) {
        this.platform = platform;
        this.symbol = symbol;
        this.changePercentage = changePercentage;
    }

    // Getters and Setters
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getChangePercentage() { return changePercentage; }
    public void setChangePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HomeStockResponse that = (HomeStockResponse) o;
        return Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(changePercentage, that.changePercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, symbol, changePercentage);
    }

    @Override
    public String toString() {
        return "HomeStockResponse{" +
                "platform='" + platform + '\'' +
                ", symbol='" + symbol + '\'' +
                ", changePercentage=" + changePercentage +
                '}';
    }
}