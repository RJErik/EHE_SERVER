package com.example.ehe_server.service.intf.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.CandleDataResponse;
import com.example.ehe_server.entity.MarketCandle.Timeframe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MarketCandleServiceInterface {

    CandleDataResponse getCandleData(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime startDate,
            LocalDateTime endDate);

    List<CandleData> getUpdatedCandles(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime lastCheckTime);

    /**
     * Checks if the latest candle for a given timeframe has been modified since the last check
     * @param platformName The platform name
     * @param stockSymbol The stock symbol
     * @param timeframeStr The timeframe in string format
     * @param lastCandleTimestamp The timestamp of the last known candle
     * @param lastCandleOpen The open price of the last known candle
     * @param lastCandleHigh The high price of the last known candle
     * @param lastCandleLow The low price of the last known candle
     * @param lastCandleClose The close price of the last known candle
     * @param lastCandleVolume The volume of the last known candle
     * @return The updated candle if modified, or null if not modified
     */
    CandleData getModifiedLatestCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime lastCandleTimestamp,
            BigDecimal lastCandleOpen,
            BigDecimal lastCandleHigh,
            BigDecimal lastCandleLow,
            BigDecimal lastCandleClose,
            BigDecimal lastCandleVolume,
            LocalDateTime endDate);

    CandleData getCandleAtTimestamp(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime timestamp);

    Timeframe parseTimeframe(String timeframeStr);
}
