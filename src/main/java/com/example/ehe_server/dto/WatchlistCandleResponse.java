package com.example.ehe_server.dto;

import com.example.ehe_server.entity.MarketCandle;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class WatchlistCandleResponse {
    private Integer watchlistItemId;
    private String platform;
    private String symbol;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private MarketCandle.Timeframe timeframe;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;

    public WatchlistCandleResponse() {}

    public WatchlistCandleResponse(Integer watchlistItemId, String platform, String symbol, LocalDateTime timestamp, MarketCandle.Timeframe timeframe, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
        this.watchlistItemId = watchlistItemId;
        this.platform = platform;
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.timeframe = timeframe;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Integer getWatchlistItemId() { return watchlistItemId; }
    public void setWatchlistItemId(Integer watchlistItemId) { this.watchlistItemId = watchlistItemId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public MarketCandle.Timeframe getTimeframe() { return timeframe; }
    public void setTimeframe(MarketCandle.Timeframe timeframe) { this.timeframe = timeframe; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistCandleResponse that = (WatchlistCandleResponse) o;
        return Objects.equals(watchlistItemId, that.watchlistItemId) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(timeframe, that.timeframe) &&
                Objects.equals(open, that.open) &&
                Objects.equals(high, that.high) &&
                Objects.equals(low, that.low) &&
                Objects.equals(close, that.close) &&
                Objects.equals(volume, that.volume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(watchlistItemId, platform, symbol, timestamp, timeframe, open, high, low, close, volume);
    }
}