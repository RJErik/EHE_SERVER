package ehe_server.service.intf.alpaca;

import java.util.Set;

public interface AlpacaDataInitializationServiceInterface {

    /**
     * Sets up a symbol: sync historical data, then add to live WebSocket
     *
     * @param symbol Trading symbol to setup
     */
    void setupSymbol(String symbol);

    /**
     * Remove a symbol from live subscriptions
     *
     * @param symbol Trading symbol to remove
     */
    void removeSymbol(String symbol);

    /**
     * Get list of symbols currently live
     *
     * @return Set of active symbols
     */
    Set<String> getLiveSymbols();

    /**
     * Get sync status for a symbol
     *
     * @param symbol Trading symbol to check
     * @return true if currently syncing historical data
     */
    boolean isSyncing(String symbol);
}