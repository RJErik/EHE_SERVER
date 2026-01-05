package com.example.ehe_server.dto;

import java.util.List;

public class CandlesResponse {

    private String platform;
    private String stockSymbol;
    private String timeframe;
    private Integer totalCandles;
    private List<CandleDTO> candles;

    public CandlesResponse() {}

    public CandlesResponse(String platform, String stockSymbol, String timeframe,
                           Integer totalCandles, List<CandleDTO> candles) {
        this.platform = platform;
        this.stockSymbol = stockSymbol;
        this.timeframe = timeframe;
        this.totalCandles = totalCandles;
        this.candles = candles;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public Integer getTotalCandles() {
        return totalCandles;
    }

    public void setTotalCandles(Integer totalCandles) {
        this.totalCandles = totalCandles;
    }

    public List<CandleDTO> getCandles() {
        return candles;
    }

    public void setCandles(List<CandleDTO> candles) {
        this.candles = candles;
    }
}