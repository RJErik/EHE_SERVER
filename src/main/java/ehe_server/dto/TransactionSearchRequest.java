package ehe_server.dto;

import ehe_server.annotation.validation.MinValue;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.entity.Transaction;
import ehe_server.exception.custom.InvalidPageNumberException;
import ehe_server.exception.custom.InvalidPageSizeException;
import ehe_server.exception.custom.MissingPageNumberException;
import ehe_server.exception.custom.MissingPageSizeException;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSearchRequest {

    private Integer userId;
    private Integer portfolioId;
    private String platform;
    private String symbol;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toTime;

    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal fromPrice;
    private BigDecimal toPrice;
    private Transaction.TransactionType type;
    private Transaction.Status status;
    @NotNullField(exception = MissingPageSizeException.class)
    @MinValue(exception = InvalidPageSizeException.class,
            min = 1)
    private Integer size;
    @NotNullField(exception = MissingPageNumberException.class)
    @MinValue(exception = InvalidPageNumberException.class,
            min = 0)
    private Integer page;

    public TransactionSearchRequest() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
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

    public LocalDateTime getFromTime() {
        return fromTime;
    }

    public void setFromTime(LocalDateTime fromTime) {
        this.fromTime = fromTime;
    }

    public LocalDateTime getToTime() {
        return toTime;
    }

    public void setToTime(LocalDateTime toTime) {
        this.toTime = toTime;
    }

    public BigDecimal getFromAmount() {
        return fromAmount;
    }

    public void setFromAmount(BigDecimal fromAmount) {
        this.fromAmount = fromAmount;
    }

    public BigDecimal getToAmount() {
        return toAmount;
    }

    public void setToAmount(BigDecimal toAmount) {
        this.toAmount = toAmount;
    }

    public BigDecimal getFromPrice() {
        return fromPrice;
    }

    public void setFromPrice(BigDecimal fromPrice) {
        this.fromPrice = fromPrice;
    }

    public BigDecimal getToPrice() {
        return toPrice;
    }

    public void setToPrice(BigDecimal toPrice) {
        this.toPrice = toPrice;
    }

    public Transaction.TransactionType getType() {
        return type;
    }

    public void setType(Transaction.TransactionType type) {
        this.type = type;
    }

    public Transaction.Status getStatus() {
        return status;
    }

    public void setStatus(Transaction.Status status) {
        this.status = status;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}