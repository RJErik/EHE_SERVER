package com.example.ehe_server.service.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockCandleSubscription {

    private final String id;
    private final Integer userId;
    private final String sessionId;
    private final String platformName;
    private final String stockSymbol;
    private final String timeframe;
    private final String destination;

    private volatile LocalDateTime latestCandleTimestamp;
    private volatile BigDecimal latestCandleOpen;
    private volatile BigDecimal latestCandleHigh;
    private volatile BigDecimal latestCandleLow;
    private volatile BigDecimal latestCandleClose;
    private volatile BigDecimal latestCandleVolume;

    public StockCandleSubscription(
            String id,
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.platformName = platformName;
        this.stockSymbol = stockSymbol;
        this.timeframe = timeframe;
        this.destination = destination;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getLatestCandleTimestamp() {
        return latestCandleTimestamp;
    }

    public BigDecimal getLatestCandleOpen() {
        return latestCandleOpen;
    }

    public BigDecimal getLatestCandleHigh() {
        return latestCandleHigh;
    }

    public BigDecimal getLatestCandleLow() {
        return latestCandleLow;
    }

    public BigDecimal getLatestCandleClose() {
        return latestCandleClose;
    }

    public BigDecimal getLatestCandleVolume() {
        return latestCandleVolume;
    }

    public void updateLatestCandle(CandleData candle) {
        if (candle != null) {
            this.latestCandleTimestamp = candle.getTimestamp();
            this.latestCandleOpen = candle.getOpenPrice();
            this.latestCandleHigh = candle.getHighPrice();
            this.latestCandleLow = candle.getLowPrice();
            this.latestCandleClose = candle.getClosePrice();
            this.latestCandleVolume = candle.getVolume();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockCandleSubscription that = (StockCandleSubscription) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StockCandleSubscription{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", platformName='" + platformName + '\'' +
                ", stockSymbol='" + stockSymbol + '\'' +
                ", timeframe='" + timeframe + '\'' +
                '}';
    }
}