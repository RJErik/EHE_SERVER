package com.example.ehe_server.service.intf.trade;

import java.math.BigDecimal;
import java.util.Map;

public interface TradingServiceInterface {
    /**
     * Execute a market order to buy or sell a stock
     *
     * @param portfolioId The ID of the portfolio
     * @param stockSymbol The symbol of the stock to trade
     * @param action "BUY" or "SELL"
     * @param amount The amount to trade
     * @param quantityType "QUANTITY" (for selling) or "QUOTE_ORDER_QTY" (for buying)
     * @return Map containing success status and trade details or error message
     */
    Map<String, Object> executeMarketOrder(Integer portfolioId, String stockSymbol, String action,
                                           BigDecimal amount, String quantityType, Integer explicitUserId);
}
