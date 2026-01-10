package ehe_server.service.intf.stock.websocket;

import ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import ehe_server.exception.custom.InvalidSubscriptionIdException;
import ehe_server.exception.custom.SubscriptionNotFoundException;

/**
 * Interface for managing WebSocket subscriptions to stock candle data updates.
 * Handles subscription lifecycle, real-time candle updates, and heartbeat messages.
 */
public interface StockWebSocketSubscriptionManagerInterface {

    /**
     * Create a new subscription for stock candle data and send the latest candle immediately.
     *
     * @param userId The ID of the user creating the subscription
     * @param sessionId The WebSocket session ID
     * @param platformName The trading platform name (e.g., "BINANCE")
     * @param stockSymbol The stock symbol to subscribe to (e.g., "BTCUSDT")
     * @param timeframe The candle timeframe (e.g., "M1", "H1", "D1")
     * @param destination The destination endpoint for candle updates
     * @return StockCandleSubscriptionResponse containing the subscription ID
     */
    StockCandleSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination);

    /**
     * Cancel a subscription (explicit unsubscribe).
     *
     * @param subscriptionId The ID of the subscription to cancel
     * @throws InvalidSubscriptionIdException if subscriptionId is null
     * @throws SubscriptionNotFoundException if subscription doesn't exist
     */
    void cancelSubscription(String subscriptionId);
}