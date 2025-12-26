package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.websocket.AlertSubscriptionResponse;
import com.example.ehe_server.exception.custom.InvalidSubscriptionIdException;
import com.example.ehe_server.exception.custom.SubscriptionNotFoundException;

/**
 * Interface for managing WebSocket subscriptions to alert notifications.
 * Handles subscription lifecycle, alert monitoring, and notification delivery.
 */
public interface AlertWebSocketSubscriptionManagerInterface {

    /**
     * Create a new subscription for alerts and return its ID.
     *
     * @param userId The ID of the user creating the subscription
     * @param sessionId The WebSocket session ID
     * @param destination The destination endpoint for alert notifications
     * @return AlertSubscriptionResponse containing the subscription ID
     */
    AlertSubscriptionResponse createSubscription(Integer userId, String sessionId, String destination);

    /**
     * Cancel a subscription (explicit unsubscribe).
     *
     * @param subscriptionId The ID of the subscription to cancel
     * @throws InvalidSubscriptionIdException if subscriptionId is null
     * @throws SubscriptionNotFoundException if subscription doesn't exist
     */
    void cancelSubscription(String subscriptionId);

    /**
     * Scheduled task to check for new alerts every minute.
     * This method is called automatically by the scheduler.
     */
    void checkForNewAlerts();
}