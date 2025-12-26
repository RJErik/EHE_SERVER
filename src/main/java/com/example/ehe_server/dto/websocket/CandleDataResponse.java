package com.example.ehe_server.dto.websocket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CandleDataResponse {
    private boolean success;
    private String message;
    private String platformName;
    private String stockSymbol;
    private String timeframe;
    private List<CandleData> candles;
    private LocalDateTime lastUpdateTime;
    private String subscriptionId; // Add this field

    // Inner class for candle data
    public static class CandleData {
        private LocalDateTime timestamp;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal volume;
        private Long sequence; // Added sequence field

        // Getters and setters
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

        public BigDecimal getClosePrice() {
            return closePrice;
        }

        public void setClosePrice(BigDecimal closePrice) {
            this.closePrice = closePrice;
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

    // Getters and setters for main class
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    // Other getters and setters...
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public List<CandleData> getCandles() {
        return candles;
    }

    public void setCandles(List<CandleData> candles) {
        this.candles = candles;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
}
