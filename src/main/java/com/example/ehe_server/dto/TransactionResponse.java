package com.example.ehe_server.dto;

import com.example.ehe_server.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class TransactionResponse {
    private Integer transactionId;
    private Integer userId;
    private Integer portfolioId;
    private String platform;
    private String symbol;
    private Transaction.TransactionType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private Transaction.Status status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionDate;

    public TransactionResponse() {}

    public TransactionResponse(Integer transactionId, Integer userId, Integer portfolioId,
                               String platform, String symbol, Transaction.TransactionType type, BigDecimal quantity,
                               BigDecimal price, BigDecimal totalValue, Transaction.Status status, LocalDateTime transactionDate) {
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

    public Transaction.TransactionType getType() { return type; }
    public void setType(Transaction.TransactionType type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public Transaction.Status getStatus() { return status; }
    public void setStatus(Transaction.Status status) { this.status = status; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionResponse that = (TransactionResponse) o;
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