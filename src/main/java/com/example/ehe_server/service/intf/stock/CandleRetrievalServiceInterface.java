package com.example.ehe_server.service.intf.stock;

import com.example.ehe_server.dto.CandlesResponse;
import java.time.LocalDateTime;

public interface CandleRetrievalServiceInterface {

    /**
     * Retrieves candles by sequence number range for a specific platform, stock and timeframe
     *
     * @param platform The trading platform name
     * @param stockSymbol The stock symbol
     * @param timeframe The candle timeframe (1m, 5m, 15m, 1h, 4h, 1d)
     * @param fromSequence Starting sequence number (inclusive)
     * @param toSequence Ending sequence number (inclusive)
     * @return CandlesResponse containing the list of candles
     */
    CandlesResponse getCandlesBySequence(String platform, String stockSymbol, String timeframe,
                                         Long fromSequence, Long toSequence);

    /**
     * Retrieves candles by date range for a specific platform, stock and timeframe
     *
     * @param platform The trading platform name
     * @param stockSymbol The stock symbol
     * @param timeframe The candle timeframe (1m, 5m, 15m, 1h, 4h, 1d)
     * @param fromDate Starting date (inclusive)
     * @param toDate Ending date (inclusive)
     * @return CandlesResponse containing the list of candles
     */
    CandlesResponse getCandlesByDate(String platform, String stockSymbol, String timeframe,
                                     LocalDateTime fromDate, LocalDateTime toDate);
}