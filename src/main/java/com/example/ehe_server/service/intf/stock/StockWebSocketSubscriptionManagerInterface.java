package com.example.ehe_server.service.intf.stock;

import com.example.ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import com.example.ehe_server.exception.custom.InvalidSubscriptionIdException;
import com.example.ehe_server.exception.custom.MissingStockSubscriptionParametersException;
import com.example.ehe_server.exception.custom.SubscriptionNotFoundException;

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
     * @throws MissingStockSubscriptionParametersException if any required parameters are missing or invalid
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

    /**
     * Scheduled task to check for candle updates and send them to clients.
     * Also validates active subscriptions and sends heartbeats to maintain connections.
     * Runs every 10 seconds.
     * This method is called automatically by the scheduler.
     */
    void checkForUpdatesAndSendHeartbeats();
}