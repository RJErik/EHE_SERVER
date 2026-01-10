package ehe_server.service.intf.alpaca;

import org.springframework.http.ResponseEntity;
import java.time.ZonedDateTime;

public interface AlpacaDataApiClientInterface {

    /**
     * Gets historical bars for a stock symbol
     * Uses /v2/stocks/{symbol}/bars endpoint
     *
     * @param symbol Stock symbol
     * @param timeframe Timeframe for bars (e.g., "1Min", "1Hour")
     * @param start Start time
     * @param end End time
     * @param pageToken Pagination token
     * @return Response containing bar data
     */
    ResponseEntity<String> getStockBars(String symbol, String timeframe,
                                        ZonedDateTime start, ZonedDateTime end,
                                        String pageToken);

    /**
     * Gets historical bars for a crypto symbol
     * Uses /v1beta3/crypto/us/bars endpoint
     *
     * @param symbol Crypto symbol (e.g., "BTC/USD")
     * @param timeframe Timeframe for bars
     * @param start Start time
     * @param end End time
     * @param pageToken Pagination token
     * @return Response containing bar data
     */
    ResponseEntity<String> getCryptoBars(String symbol, String timeframe,
                                         ZonedDateTime start, ZonedDateTime end,
                                         String pageToken);

    /**
     * Automatically detects if symbol is crypto (contains "/") and calls appropriate endpoint
     *
     * @param symbol Trading symbol
     * @param timeframe Timeframe for bars
     * @param start Start time
     * @param end End time
     * @param pageToken Pagination token
     * @return Response containing bar data
     */
    ResponseEntity<String> getBars(String symbol, String timeframe,
                                   ZonedDateTime start, ZonedDateTime end,
                                   String pageToken);
}