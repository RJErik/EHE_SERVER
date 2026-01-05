package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CandleDTO {

    private Integer candleId;
    private LocalDateTime timestamp;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private Long sequence; // New field

    public CandleDTO() {}

    public CandleDTO(Integer candleId, LocalDateTime timestamp, BigDecimal openPrice,
                     BigDecimal closePrice, BigDecimal highPrice, BigDecimal lowPrice,
                     BigDecimal volume, Long sequence) {
        this.candleId = candleId;
        this.timestamp = timestamp;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.sequence = sequence;
    }

    public Integer getCandleId() {
        return candleId;
    }

    public void setCandleId(Integer candleId) {
        this.candleId = candleId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
}