// src/main/java/com/example/ehe_server/dto/portfolio/StockDetails.java
package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class StockDetails {
    private String symbol;
    private BigDecimal value;

    public StockDetails() {}

    public StockDetails(String symbol, BigDecimal value) {
        this.symbol = symbol;
        this.value = value;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockDetails that = (StockDetails) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, value);
    }
}