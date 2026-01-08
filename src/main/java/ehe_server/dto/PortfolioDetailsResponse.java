// src/main/java/com/example/ehe_server/dto/portfolio/PortfolioDetailsResponse.java
package ehe_server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class PortfolioDetailsResponse {
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationDate;
    private String platform;
    private CashDetails reservedCash;
    private List<StockDetails> stocks;
    private BigDecimal totalValue;

    public PortfolioDetailsResponse() {}

    public PortfolioDetailsResponse(String name, LocalDateTime creationDate, String platform, CashDetails reservedCash, List<StockDetails> stocks, BigDecimal totalValue) {
        this.name = name;
        this.creationDate = creationDate;
        this.platform = platform;
        this.reservedCash = reservedCash;
        this.stocks = stocks;
        this.totalValue = totalValue;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public CashDetails getReservedCash() { return reservedCash; }
    public void setReservedCash(CashDetails reservedCash) { this.reservedCash = reservedCash; }
    public List<StockDetails> getStocks() { return stocks; }
    public void setStocks(List<StockDetails> stocks) { this.stocks = stocks; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioDetailsResponse that = (PortfolioDetailsResponse) o;
        return Objects.equals(name, that.name) && Objects.equals(creationDate, that.creationDate) && Objects.equals(platform, that.platform) && Objects.equals(reservedCash, that.reservedCash) && Objects.equals(stocks, that.stocks) && Objects.equals(totalValue, that.totalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, creationDate, platform, reservedCash, stocks, totalValue);
    }
}