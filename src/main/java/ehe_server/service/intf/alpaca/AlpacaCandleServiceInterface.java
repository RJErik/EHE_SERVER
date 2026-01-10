package ehe_server.service.intf.alpaca;

import com.fasterxml.jackson.databind.JsonNode;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import java.util.List;

public interface AlpacaCandleServiceInterface {

    /**
     * Syncs historical data for a symbol
     * This will fetch all available historical data from the earliest point to now
     *
     * @param symbol Trading symbol to sync
     */
    void syncHistoricalData(String symbol);

    /**
     * Saves a batch of candles and their aggregations in a single transaction
     *
     * @param stock Platform stock entity
     * @param candles List of candles to save
     */
    void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles);

    /**
     * Processes a real-time candle from WebSocket
     *
     * @param candleData JSON data from WebSocket
     * @param stock Platform stock entity
     */
    void processRealtimeCandle(JsonNode candleData, PlatformStock stock);
}