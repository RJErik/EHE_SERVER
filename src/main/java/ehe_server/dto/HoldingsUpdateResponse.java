package ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class HoldingsUpdateResponse {
    private Integer updatedCount;
    private BigDecimal reservedCash;

    public HoldingsUpdateResponse() {}

    public HoldingsUpdateResponse(Integer updatedCount, BigDecimal reservedCash) {
        this.updatedCount = updatedCount;
        this.reservedCash = reservedCash;
    }

    public Integer getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(Integer updatedCount) { this.updatedCount = updatedCount; }
    public BigDecimal getReservedCash() { return reservedCash; }
    public void setReservedCash(BigDecimal reservedCash) { this.reservedCash = reservedCash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HoldingsUpdateResponse that = (HoldingsUpdateResponse) o;
        return Objects.equals(updatedCount, that.updatedCount) &&
                Objects.equals(reservedCash, that.reservedCash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updatedCount, reservedCash);
    }
}