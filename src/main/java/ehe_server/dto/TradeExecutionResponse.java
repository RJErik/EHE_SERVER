package ehe_server.dto;

import ehe_server.entity.Transaction;

import java.math.BigDecimal;
import java.util.Objects;

public class TradeExecutionResponse {
    private Integer transactionId;
    private String symbol;
    private Transaction.TransactionType type;
    private BigDecimal quantity;
    private Transaction.Status status;

    public TradeExecutionResponse() {}

    public TradeExecutionResponse(Integer orderId, String symbol, Transaction.TransactionType side, BigDecimal executedQty, Transaction.Status status) {
        this.transactionId = orderId;
        this.symbol = symbol;
        this.type = side;
        this.quantity = executedQty;
        this.status = status;
    }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Transaction.TransactionType getType() { return type; }
    public void setType(Transaction.TransactionType type) { this.type = type; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Transaction.Status getStatus() { return status; }
    public void setStatus(Transaction.Status status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeExecutionResponse that = (TradeExecutionResponse) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(type, that.type) &&
                Objects.equals(quantity, that.quantity) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, symbol, type, quantity, status);
    }
}