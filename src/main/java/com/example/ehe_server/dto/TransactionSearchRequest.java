package com.example.ehe_server.dto;

import java.math.BigDecimal;

public class TransactionSearchRequest {
    private Integer userId;
    private Integer portfolioId;
    private String platform;
    private String symbol;
    private String fromTime;
    private String toTime;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal fromPrice;
    private BigDecimal toPrice;
    private String type;
    private String status;

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getFromTime() { return fromTime; }
    public void setFromTime(String fromTime) { this.fromTime = fromTime; }

    public String getToTime() { return toTime; }
    public void setToTime(String toTime) { this.toTime = toTime; }

    public BigDecimal getFromAmount() { return fromAmount; }
    public void setFromAmount(BigDecimal fromAmount) { this.fromAmount = fromAmount; }

    public BigDecimal getToAmount() { return toAmount; }
    public void setToAmount(BigDecimal toAmount) { this.toAmount = toAmount; }

    public BigDecimal getFromPrice() { return fromPrice; }
    public void setFromPrice(BigDecimal fromPrice) { this.fromPrice = fromPrice; }

    public BigDecimal getToPrice() { return toPrice; }
    public void setToPrice(BigDecimal toPrice) { this.toPrice = toPrice; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}