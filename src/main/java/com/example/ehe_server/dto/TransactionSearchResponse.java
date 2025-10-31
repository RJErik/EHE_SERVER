package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class TransactionSearchResponse {
    private Integer transactionId;
    private Integer userId;
    private Integer portfolioId;
    private String platform;
    private String symbol;
    private String type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private String status;
    private String transactionDate;

    public TransactionSearchResponse() {}

    public TransactionSearchResponse(Integer transactionId, Integer userId, Integer portfolioId,
                                     String platform, String symbol, String type, BigDecimal quantity,
                                     BigDecimal price, BigDecimal totalValue, String status, String transactionDate) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.platform = platform;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = totalValue;
        this.status = status;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionSearchResponse that = (TransactionSearchResponse) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(portfolioId, that.portfolioId) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(type, that.type) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(price, that.price) &&
                Objects.equals(totalValue, that.totalValue) &&
                Objects.equals(status, that.status) &&
                Objects.equals(transactionDate, that.transactionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, userId, portfolioId, platform, symbol, type, quantity, price, totalValue, status, transactionDate);
    }
}