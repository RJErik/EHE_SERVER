package ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class TradeExecutionResponse {
    private Integer orderId;
    private String symbol;
    private String side;
    private BigDecimal origQty;
    private BigDecimal executedQty;
    private BigDecimal cumulativeQuoteQty;
    private String status;

    public TradeExecutionResponse() {}

    public TradeExecutionResponse(Integer orderId, String symbol, String side, BigDecimal origQty, BigDecimal executedQty, BigDecimal cummulativeQuoteQty, String status) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.origQty = origQty;
        this.executedQty = executedQty;
        this.cumulativeQuoteQty = cummulativeQuoteQty;
        this.status = status;
    }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getOrigQty() { return origQty; }
    public void setOrigQty(BigDecimal origQty) { this.origQty = origQty; }
    public BigDecimal getExecutedQty() { return executedQty; }
    public void setExecutedQty(BigDecimal executedQty) { this.executedQty = executedQty; }
    public BigDecimal getCumulativeQuoteQty() { return cumulativeQuoteQty; }
    public void setCumulativeQuoteQty(BigDecimal cumulativeQuoteQty) { this.cumulativeQuoteQty = cumulativeQuoteQty; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeExecutionResponse that = (TradeExecutionResponse) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(side, that.side) &&
                Objects.equals(origQty, that.origQty) &&
                Objects.equals(executedQty, that.executedQty) &&
                Objects.equals(cumulativeQuoteQty, that.cumulativeQuoteQty) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, symbol, side, origQty, executedQty, cumulativeQuoteQty, status);
    }
}