// src/main/java/com/example/ehe_server/dto/HoldingDetails.java
        package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class HoldingDetails {
    private Integer id;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal valueInUsdt;

    public HoldingDetails() {}

    public HoldingDetails(Integer id, String symbol, BigDecimal quantity, BigDecimal valueInUsdt) {
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.valueInUsdt = valueInUsdt;
    }

    // Getters, Setters, and standard boilerplate (equals, hashCode)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getValueInUsdt() { return valueInUsdt; }
    public void setValueInUsdt(BigDecimal valueInUsdt) { this.valueInUsdt = valueInUsdt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HoldingDetails that = (HoldingDetails) o;
        return Objects.equals(id, that.id) && Objects.equals(symbol, that.symbol) && Objects.equals(quantity, that.quantity) && Objects.equals(valueInUsdt, that.valueInUsdt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, quantity, valueInUsdt);
    }
}