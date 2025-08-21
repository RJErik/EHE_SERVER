package com.example.ehe_server.dto;

import java.util.Objects;

public class WatchlistRetrievalResponse {
    private Integer id;
    private String platform;
    private String symbol;
    private String dateAdded;

    public WatchlistRetrievalResponse() {}

    public WatchlistRetrievalResponse(Integer id, String platform, String symbol, String dateAdded) {
        this.id = id;
        this.platform = platform;
        this.symbol = symbol;
        this.dateAdded = dateAdded;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistRetrievalResponse that = (WatchlistRetrievalResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(dateAdded, that.dateAdded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform, symbol, dateAdded);
    }
}