package com.example.ehe_server.controller;

import com.example.ehe_server.service.alert.AlertWebSocketSubscriptionManager;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AlertWebSocketController {

    private final AlertWebSocketSubscriptionManager alertWebSocketSubscriptionManager;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public AlertWebSocketController(
            AlertWebSocketSubscriptionManager alertWebSocketSubscriptionManager,
            LoggingServiceInterface loggingService,
            UserContextService userContextService) {
        this.alertWebSocketSubscriptionManager = alertWebSocketSubscriptionManager;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @MessageMapping("/alerts/subscribe")
    @SendToUser("/queue/alerts")
    public Map<String, Object> subscribeToAlerts() {

        Map<String, Object> response = new HashMap<>();

        try {
            // Log subscription request
            loggingService.logAction("WebSocket subscription request for alerts");

            // Create subscription for this user
            String subscriptionId = alertWebSocketSubscriptionManager.createSubscription(
                    userContextService.getCurrentUserId().intValue(),
                    "/user/queue/alerts");

            // Return subscription details
            response.put("success", true);
            response.put("subscriptionId", subscriptionId);
            response.put("message", "Alert subscription created successfully");

        } catch (Exception e) {
            loggingService.logError("Error creating alert subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error creating alert subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/alerts/unsubscribe")
    @SendToUser("/queue/alerts")
    public Map<String, Object> unsubscribeFromAlerts(
            @Payload Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String subscriptionId = request.get("subscriptionId");

            if (subscriptionId == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            loggingService.logAction("WebSocket unsubscribe request for alert subscription " + subscriptionId);

            boolean cancelled = alertWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

            if (cancelled) {
                response.put("success", true);
                response.put("message", "Alert subscription cancelled successfully");
                response.put("subscriptionId", subscriptionId);
            } else {
                response.put("success", false);
                response.put("message", "Alert subscription not found: " + subscriptionId);
                response.put("subscriptionId", subscriptionId);
            }

        } catch (Exception e) {
            loggingService.logError("Error cancelling alert subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error cancelling alert subscription: " + e.getMessage());
        }

        return response;
    }
}
