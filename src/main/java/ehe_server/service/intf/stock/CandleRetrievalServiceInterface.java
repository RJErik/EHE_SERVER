package ehe_server.service.intf.stock;

import ehe_server.dto.CandlesResponse;
import ehe_server.entity.MarketCandle;

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
    CandlesResponse getCandlesBySequence(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
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
    CandlesResponse getCandlesByDate(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
                                     LocalDateTime fromDate, LocalDateTime toDate);
}