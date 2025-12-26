package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import com.example.ehe_server.exception.custom.InvalidSubscriptionIdException;
import com.example.ehe_server.exception.custom.SubscriptionNotFoundException;

/**
 * Interface for managing WebSocket subscriptions to automated trade notifications.
 * Handles subscription lifecycle, trade rule monitoring, and trade execution notifications.
 */
public interface AutomatedTradeWebSocketSubscriptionManagerInterface {

    /**
     * Create a new subscription for automated trade notifications and return its ID.
     *
     * @param userId The ID of the user creating the subscription
     * @param sessionId The WebSocket session ID
     * @param destination The destination endpoint for trade notifications
     * @return AutomatedTradeSubscriptionResponse containing the subscription ID
     */
    AutomatedTradeSubscriptionResponse createSubscription(Integer userId, String sessionId, String destination);

    /**
     * Cancel a subscription (explicit unsubscribe).
     *
     * @param subscriptionId The ID of the subscription to cancel
     * @throws InvalidSubscriptionIdException if subscriptionId is null
     * @throws SubscriptionNotFoundException if subscription doesn't exist
     */
    void cancelSubscription(String subscriptionId);

    /**
     * Scheduled task to check for automated trade rules every minute.
     * Evaluates all active trade rules against current market data and executes
     * trades when conditions are met.
     * This method is called automatically by the scheduler.
     */
    void checkAutomatedTradeRules();
}