package ehe_server.dto;

import java.util.List;
import java.util.Objects;

public class StocksByPlatformResponse {
    private String platformName;
    private List<String> stocks;

    public StocksByPlatformResponse() {}

    public StocksByPlatformResponse(String platformName, List<String> stocks) {
        this.platformName = platformName;
        this.stocks = stocks;
    }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public List<String> getStocks() { return stocks; }
    public void setStocks(List<String> stocks) { this.stocks = stocks; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StocksByPlatformResponse that = (StocksByPlatformResponse) o;
        return Objects.equals(platformName, that.platformName) &&
                Objects.equals(stocks, that.stocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformName, stocks);
    }
}