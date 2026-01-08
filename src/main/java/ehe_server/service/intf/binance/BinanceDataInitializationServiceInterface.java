package ehe_server.service.intf.binance;

import java.util.Set;

/**
 * Interface for Binance data initialization and maintenance
 */
public interface BinanceDataInitializationServiceInterface {

    /**
     * Initialize data synchronization on application startup
     */
    void initializeDataAsync();

    /**
     * Sets up a symbol: sync historical data, then add to live WebSocket
     *
     * @param symbol The trading pair symbol
     */
    void setupSymbol(String symbol);

    /**
     * Remove a symbol from live subscriptions
     *
     * @param symbol The trading pair symbol
     */
    void removeSymbol(String symbol);

    /**
     * Daily maintenance to sync any gaps in historical data
     */
    void performDailyMaintenance();

    /**
     * Hourly verification that WebSocket is connected with correct symbols
     */
    void verifyWebSocketConnections();

    /**
     * Get list of symbols currently live
     *
     * @return Set of live symbol names
     */
    Set<String> getLiveSymbols();

    /**
     * Get sync status for a symbol
     *
     * @param symbol The trading pair symbol
     * @return true if syncing, false otherwise
     */
    boolean isSyncing(String symbol);
}
