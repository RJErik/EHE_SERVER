// src/main/java/com/example/ehe_server/dto/portfolio/CashDetails.java
package ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class CashDetails {
    private String currency;
    private BigDecimal value;

    public CashDetails() {}

    public CashDetails(String currency, BigDecimal value) {
        this.currency = currency;
        this.value = value;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashDetails that = (CashDetails) o;
        return Objects.equals(currency, that.currency) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, value);
    }
}