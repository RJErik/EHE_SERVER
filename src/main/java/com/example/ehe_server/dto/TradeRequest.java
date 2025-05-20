package com.example.ehe_server.dto;

import java.math.BigDecimal;

public class TradeRequest {
    private Integer portfolioId;
    private String stockSymbol;
    private String action; // "BUY" or "SELL"
    private BigDecimal amount;
    private String quantityType; // "QUANTITY" or "QUOTE_ORDER_QTY"

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(String quantityType) {
        this.quantityType = quantityType;
    }
}
