package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.annotation.validation.PositiveAmount;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.Transaction;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.*;

import java.math.BigDecimal;

public class TradeRequest {
    @NotNullField(exception = MissingPortfolioIdException.class)
    private Integer portfolioId;
    @NotEmptyString(exception = MissingStockSymbolException.class)
    private String stockSymbol;
    @NotNullField(exception = MissingActionTypeException.class)
    private Transaction.TransactionType action; // "BUY" or "SELL"
    @NotNullField(exception = MissingQuantityException.class)
    @PositiveAmount(exception = InvalidQuantityException.class)
    private BigDecimal quantity;
    @NotNullField(exception = MissingQuantityTypeException.class)
    private AutomatedTradeRule.QuantityType quantityType; // "QUANTITY" or "QUOTE_ORDER_QTY"

    public Integer getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public Transaction.TransactionType getAction() {
        return action;
    }

    public void setAction(Transaction.TransactionType action) {
        this.action = action;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public AutomatedTradeRule.QuantityType getQuantityType() {
        return quantityType;
    }

    public void setQuantityType(AutomatedTradeRule.QuantityType quantityType) {
        this.quantityType = quantityType;
    }
}
