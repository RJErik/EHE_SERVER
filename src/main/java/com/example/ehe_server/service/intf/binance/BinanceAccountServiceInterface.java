package com.example.ehe_server.service.intf.binance;

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
     * @param quantity      The quantity to trade (for SELL orders)
     * @param quoteOrderQty The quote currency amount to spend (for BUY orders)
     * @return Response from Binance API
     */
    Map<String, Object> placeMarketOrder(
            String apiKey,
            String secretKey,
            String symbol,
            String side,
            String type,
            BigDecimal quantity,
            BigDecimal quoteOrderQty
    );
}