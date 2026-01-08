package ehe_server.service.intf.trade;

import ehe_server.dto.TradeExecutionResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.Transaction;

import java.math.BigDecimal;

public interface TradingServiceInterface {
    /**
     * Execute a market order to buy or sell a stock
     * @param userId The user that has initiated the service
     * @param portfolioId The ID of the portfolio
     * @param stockSymbol The symbol of the stock to trade
     * @param action "BUY" or "SELL"
     * @param amount The amount to trade
     * @param quantityType "QUANTITY" (for selling) or "QUOTE_ORDER_QTY" (for buying)
     * @return Map containing success status and trade details or error message
     */
    TradeExecutionResponse executeTrade(Integer userId, Integer portfolioId, String stockSymbol,
                                        Transaction.TransactionType action, BigDecimal amount,
                                        AutomatedTradeRule.QuantityType quantityType);
}
