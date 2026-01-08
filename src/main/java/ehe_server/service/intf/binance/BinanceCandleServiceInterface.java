package ehe_server.service.intf.binance;

import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Interface for managing Binance candle data
 */
public interface BinanceCandleServiceInterface {

    /**
     * Synchronizes historical candle data for a symbol
     * Finds earliest available data and fetches up to present
     *
     * @param symbol The trading pair symbol
     */
    void syncHistoricalData(String symbol);

    /**
     * Saves a batch of candles and their aggregations in a transaction
     *
     * @param stock   The platform stock entity
     * @param candles The list of minute candles to save and aggregate
     */
    void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles);

    /**
     * Processes real-time candle data from WebSocket
     *
     * @param candleData The candle data from WebSocket
     * @param stock      The platform stock entity
     */
    void processRealtimeCandle(JsonNode candleData, PlatformStock stock);
}