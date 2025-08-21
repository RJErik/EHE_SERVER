// src/main/java/com/example/ehe_server/dto/PortfolioValueResponse.java
package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class PortfolioValueResponse {
    private Integer portfolioId;
    private String portfolioName;
    private BigDecimal totalValue;
    private BigDecimal reservedCash;
    private List<HoldingDetails> holdings;

    public PortfolioValueResponse() {}

    public PortfolioValueResponse(Integer portfolioId, String portfolioName, BigDecimal totalValue, BigDecimal reservedCash, List<HoldingDetails> holdings) {
        this.portfolioId = portfolioId;
        this.portfolioName = portfolioName;
        this.totalValue = totalValue;
        this.reservedCash = reservedCash;
        this.holdings = holdings;
    }

    // Getters, Setters, and standard boilerplate (equals, hashCode)
    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getReservedCash() { return reservedCash; }
    public void setReservedCash(BigDecimal reservedCash) { this.reservedCash = reservedCash; }
    public List<HoldingDetails> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingDetails> holdings) { this.holdings = holdings; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioValueResponse that = (PortfolioValueResponse) o;
        return Objects.equals(portfolioId, that.portfolioId) && Objects.equals(portfolioName, that.portfolioName) && Objects.equals(totalValue, that.totalValue) && Objects.equals(reservedCash, that.reservedCash) && Objects.equals(holdings, that.holdings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, portfolioName, totalValue, reservedCash, holdings);
    }
}