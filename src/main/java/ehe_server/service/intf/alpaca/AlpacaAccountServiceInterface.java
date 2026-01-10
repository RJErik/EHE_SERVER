package ehe_server.service.intf.alpaca;

import ehe_server.entity.AutomatedTradeRule;
import java.math.BigDecimal;
import java.util.Map;

public interface AlpacaAccountServiceInterface {

    /**
     * Gets account information from Alpaca
     *
     * @return Map containing account information
     */
    Map<String, Object> getAccountInfo();

    /**
     * Places a market order on Alpaca
     *
     * @param symbol Trading symbol (e.g., "AAPL" or "BTC/USD")
     * @param side "buy" or "sell"
     * @param amount The amount to trade
     * @param timeInForce "day", "gtc", "ioc", "fok"
     * @param quantityType QUANTITY for shares/coins, QUOTE_ORDER_QTY for dollar amount
     * @return Response from Alpaca API
     */
    Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal amount,
                                         String timeInForce, AutomatedTradeRule.QuantityType quantityType);
}