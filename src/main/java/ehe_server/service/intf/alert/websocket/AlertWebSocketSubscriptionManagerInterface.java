package ehe_server.service.intf.alert.websocket;

import ehe_server.dto.websocket.AlertSubscriptionResponse;

public interface AlertWebSocketSubscriptionManagerInterface {

    /**
     * Create a new subscription for alerts.
     *
     * @param userId      the user ID
     * @param sessionId   the WebSocket session ID
     * @param destination the destination queue for notifications
     * @return the subscription response containing the subscription ID
     */
    AlertSubscriptionResponse createSubscription(Integer userId, String sessionId, String destination);

    /**
     * Cancel an existing subscription.
     *
     * @param subscriptionId the subscription ID to cancel
     */
    void cancelSubscription(String subscriptionId);
}