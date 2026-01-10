package ehe_server.service.intf.binance;

import ehe_server.entity.AutomatedTradeRule;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface for Binance account operations
 */
public interface BinanceAccountServiceInterface {

    /**
     * Retrieves account information from Binance
     *
     * @param apiKey    The API key
     * @param secretKey The secret key
     * @return Account information or error response
     */
    Map<String, Object> getAccountInfo(String apiKey, String secretKey);

    /**
     * Places a market order on Binance
     *
     * @param apiKey        The API key
     * @param secretKey     The secret key
     * @param symbol        The trading pair symbol (e.g., "BTCUSDT")
     * @param side          "BUY" or "SELL"
     * @param type          Order type (e.g., "MARKET")
     * @param quantity      The quantity to trade
     * @param quantityType  The quantity type (e.g., QUANTITY, QUOTE_ORDER_QTY)
     * @return Response from Binance API
     */
    Map<String, Object> placeMarketOrder(
            String apiKey,
            String secretKey,
            String symbol,
            String side,
            String type,
            BigDecimal quantity,
            AutomatedTradeRule.QuantityType quantityType
    );
}