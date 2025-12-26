package com.example.ehe_server.service.intf.binance;

import org.springframework.http.ResponseEntity;

/**
 * Interface for Binance API communication
 */
public interface BinanceApiClientInterface {

    /**
     * Retrieves kline/candlestick data from Binance
     *
     * @param symbol    Trading pair symbol
     * @param interval  Time interval (e.g., "1m", "5m", "1h")
     * @param startTime Start time in milliseconds (optional)
     * @param endTime   End time in milliseconds (optional)
     * @param limit     Number of candles to retrieve (optional, max 1000)
     * @return Response containing kline data
     */
    ResponseEntity<String> getKlines(
            String symbol,
            String interval,
            Long startTime,
            Long endTime,
            Integer limit
    );

    /**
     * Check if the API client is currently rate limited
     *
     * @param requestKey The request identifier
     * @return true if rate limited, false otherwise
     */
    default boolean isRateLimited(String requestKey) {
        return false;
    }
}