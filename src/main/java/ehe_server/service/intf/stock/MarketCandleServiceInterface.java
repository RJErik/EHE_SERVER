package ehe_server.service.intf.stock;

import ehe_server.dto.websocket.CandleDataResponse.CandleData;
import ehe_server.entity.MarketCandle.Timeframe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MarketCandleServiceInterface {

    /**
     * Get the latest candle for a given stock and timeframe
     */
    CandleData getLatestCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr);

    /**
     * Check if the candle at a specific timestamp has been modified
     * Returns the updated candle if modified, null otherwise
     */
    CandleData getModifiedCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime candleTimestamp,
            BigDecimal lastOpen,
            BigDecimal lastHigh,
            BigDecimal lastLow,
            BigDecimal lastClose,
            BigDecimal lastVolume);

    /**
     * Parse the timeframe string into an enum
     */
    Timeframe parseTimeframe(String timeframeStr);
}