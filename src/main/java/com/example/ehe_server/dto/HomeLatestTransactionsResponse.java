package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class HomeLatestTransactionsResponse {
    private String pseudonym;
    private String platform;
    private String symbol;
    private BigDecimal amount;
    private String type;

    public HomeLatestTransactionsResponse(String pseudonym, String platform, String symbol, BigDecimal amount, String type) {
        this.pseudonym = pseudonym;
        this.platform = platform;
        this.symbol = symbol;
        this.amount = amount;
        this.type = type;
    }

    public HomeLatestTransactionsResponse() {
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public void setPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HomeLatestTransactionsResponse that = (HomeLatestTransactionsResponse) o;
        return Objects.equals(pseudonym, that.pseudonym) && Objects.equals(platform, that.platform) && Objects.equals(symbol, that.symbol) && Objects.equals(amount, that.amount) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pseudonym, platform, symbol, amount, type);
    }

    @Override
    public String toString() {
        return "HomeLatestTransactionsResponse{" +
                "pseudonym='" + pseudonym + '\'' +
                ", platform='" + platform + '\'' +
                ", symbol='" + symbol + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                '}';
    }
}
