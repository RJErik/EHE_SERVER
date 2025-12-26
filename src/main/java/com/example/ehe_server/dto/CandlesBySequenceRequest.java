package com.example.ehe_server.dto;

public class CandlesBySequenceRequest {

    private String platform;

    private String stockSymbol;

    private String timeframe;

    private Long fromSequence;

    private Long toSequence;

    // Constructors
    public CandlesBySequenceRequest() {}

    public CandlesBySequenceRequest(String platform, String stockSymbol, String timeframe,
                                    Long fromSequence, Long toSequence) {
        this.platform = platform;
        this.stockSymbol = stockSymbol;
        this.timeframe = timeframe;
        this.fromSequence = fromSequence;
        this.toSequence = toSequence;
    }

    // Getters and setters
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

    public Long getFromSequence() {
        return fromSequence;
    }

    public void setFromSequence(Long fromSequence) {
        this.fromSequence = fromSequence;
    }

    public Long getToSequence() {
        return toSequence;
    }

    public void setToSequence(Long toSequence) {
        this.toSequence = toSequence;
    }
}