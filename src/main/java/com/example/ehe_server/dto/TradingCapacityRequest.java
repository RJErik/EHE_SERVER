package com.example.ehe_server.dto;

public class TradingCapacityRequest {
    private Integer portfolioId;
    private String stockSymbol;

    // Getters and setters
    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }
}
