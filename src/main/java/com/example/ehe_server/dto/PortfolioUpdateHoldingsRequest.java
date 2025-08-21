package com.example.ehe_server.dto;

import java.util.Objects;

public class PortfolioUpdateHoldingsRequest {
    private Integer portfolioId;

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioUpdateHoldingsRequest that = (PortfolioUpdateHoldingsRequest) o;
        return Objects.equals(portfolioId, that.portfolioId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(portfolioId);
    }
}
