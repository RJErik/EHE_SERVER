package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class HomeWorstStockResponse {
    private String platform;
    private String symbol;
    private BigDecimal changePercentage;

    public HomeWorstStockResponse(String platform, String symbol, BigDecimal changePercentage) {
        this.platform = platform;
        this.symbol = symbol;
        this.changePercentage = changePercentage;
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

    public BigDecimal getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(BigDecimal changePercentage) {
        this.changePercentage = changePercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HomeWorstStockResponse that = (HomeWorstStockResponse) o;
        return Objects.equals(platform, that.platform) && Objects.equals(symbol, that.symbol) && Objects.equals(changePercentage, that.changePercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, symbol, changePercentage);
    }

    @Override
    public String toString() {
        return "HomeBestStockResponse{" +
                "platform='" + platform + '\'' +
                ", symbol='" + symbol + '\'' +
                ", changePercentage=" + changePercentage +
                '}';
    }
}
