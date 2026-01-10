package ehe_server.service.intf.alpaca;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Consumer;

public interface AlpacaWebSocketClientInterface {

    /**
     * Update subscriptions with a complete list of symbols
     * This will reconnect the websockets if the symbol list changes
     *
     * @param symbols List of symbols to subscribe to
     */
    void updateSubscriptions(List<String> symbols);

    /**
     * Register a message handler for a specific symbol
     *
     * @param symbol Symbol to register handler for
     * @param handler Consumer to handle incoming JsonNode messages
     */
    void registerHandler(String symbol, Consumer<JsonNode> handler);

    /**
     * Unregister a message handler for a specific symbol
     *
     * @param symbol Symbol to unregister handler for
     */
    void unregisterHandler(String symbol);

    /**
     * Check if WebSocket is currently connected and authenticated
     * Returns true if at least one feed with subscriptions is connected
     *
     * @return true if connected and authenticated
     */
    boolean isConnected();

    /**
     * Get current subscription count (total across both feeds)
     *
     * @return Total number of active subscriptions
     */
    int getSubscriptionCount();
}