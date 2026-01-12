package ehe_server.service.intf.alpaca;

import ehe_server.entity.AutomatedTradeRule;
import java.math.BigDecimal;
import java.util.Map;

public interface AlpacaAccountServiceInterface {

    /**
     * Gets account information from Alpaca
     *
     * @param apiKey    The API key
     * @param secretKey The secret key
     * @return Map containing account information
     */
    Map<String, Object> getAccountInfo(String apiKey, String secretKey);

    /**
     * Places a market order on Alpaca
     *
     * @param apiKey       The API key
     * @param secretKey    The secret key
     * @param symbol       Trading symbol (e.g., "AAPL" or "BTC/USD")
     * @param side         "buy" or "sell"
     * @param type         Order type (e.g., "market")
     * @param amount       The amount to trade
     * @param quantityType QUANTITY for shares/coins, QUOTE_ORDER_QTY for dollar amount
     * @return Response from Alpaca API
     */
    Map<String, Object> placeMarketOrder(
            String apiKey,
            String secretKey,
            String symbol,
            String side,
            String type,
            BigDecimal amount,
            AutomatedTradeRule.QuantityType quantityType
    );
}