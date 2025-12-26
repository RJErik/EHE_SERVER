package com.example.ehe_server.service.intf.binance;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interface for Binance WebSocket client operations
 */
public interface BinanceWebSocketClientInterface {

    /**
     * Update subscriptions with a complete list of symbols
     * This will reconnect the websocket if the symbol list changes
     *
     * @param symbols List of symbols to subscribe to
     */
    void updateSubscriptions(List<String> symbols);

    /**
     * Add symbols to the existing subscription
     *
     * @param symbols List of symbols to add
     */
    void addSymbols(List<String> symbols);

    /**
     * Remove symbols from the subscription
     *
     * @param symbols List of symbols to remove
     */
    void removeSymbols(List<String> symbols);

    /**
     * Register a message handler for a specific symbol
     *
     * @param symbol  The trading pair symbol
     * @param handler Consumer to handle incoming messages
     */
    void registerHandler(String symbol, Consumer<JsonNode> handler);

    /**
     * Unregister a message handler for a specific symbol
     *
     * @param symbol The trading pair symbol
     */
    void unregisterHandler(String symbol);

    /**
     * Check if WebSocket is currently connected
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Get current subscription count
     *
     * @return Number of active subscriptions
     */
    int getSubscriptionCount();

    /**
     * Get list of currently subscribed symbols
     *
     * @return Set of subscribed symbols
     */
    Set<String> getSubscribedSymbols();
}