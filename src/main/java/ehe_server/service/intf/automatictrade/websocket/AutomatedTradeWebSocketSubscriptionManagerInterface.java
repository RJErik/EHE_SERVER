package ehe_server.service.intf.automatictrade.websocket;

import ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import ehe_server.service.automatedtraderule.websocket.AutomatedTradeSubscription;

import java.util.Collection;

public interface AutomatedTradeWebSocketSubscriptionManagerInterface {

    /**
     * Create a new subscription for automated trades.
     */
    AutomatedTradeSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String destination);

    /**
     * Cancel an existing subscription.
     */
    void cancelSubscription(String subscriptionId);

    /**
     * Get all active subscriptions for a user.
     */
    Collection<AutomatedTradeSubscription> getSubscriptionsForUser(Integer userId);
}