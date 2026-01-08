package ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class TradingCapacityResponse {
    private String stockSymbol;
    private BigDecimal currentHolding;
    private BigDecimal reservedCash;
    private BigDecimal currentPrice;
    private BigDecimal maxBuyQuantity;
    private BigDecimal maxSellQuantity;

    public TradingCapacityResponse() {}

    public TradingCapacityResponse(String stockSymbol, BigDecimal currentHolding, BigDecimal reservedCash, BigDecimal currentPrice, BigDecimal maxBuyQuantity, BigDecimal maxSellQuantity) {
        this.stockSymbol = stockSymbol;
        this.currentHolding = currentHolding;
        this.reservedCash = reservedCash;
        this.currentPrice = currentPrice;
        this.maxBuyQuantity = maxBuyQuantity;
        this.maxSellQuantity = maxSellQuantity;
    }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public BigDecimal getCurrentHolding() { return currentHolding; }
    public void setCurrentHolding(BigDecimal currentHolding) { this.currentHolding = currentHolding; }
    public BigDecimal getReservedCash() { return reservedCash; }
    public void setReservedCash(BigDecimal reservedCash) { this.reservedCash = reservedCash; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getMaxBuyQuantity() { return maxBuyQuantity; }
    public void setMaxBuyQuantity(BigDecimal maxBuyQuantity) { this.maxBuyQuantity = maxBuyQuantity; }
    public BigDecimal getMaxSellQuantity() { return maxSellQuantity; }
    public void setMaxSellQuantity(BigDecimal maxSellQuantity) { this.maxSellQuantity = maxSellQuantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradingCapacityResponse that = (TradingCapacityResponse) o;
        return Objects.equals(stockSymbol, that.stockSymbol) &&
                Objects.equals(currentHolding, that.currentHolding) &&
                Objects.equals(reservedCash, that.reservedCash) &&
                Objects.equals(currentPrice, that.currentPrice) &&
                Objects.equals(maxBuyQuantity, that.maxBuyQuantity) &&
                Objects.equals(maxSellQuantity, that.maxSellQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockSymbol, currentHolding, reservedCash, currentPrice, maxBuyQuantity, maxSellQuantity);
    }
}