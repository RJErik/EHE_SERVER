package com.example.ehe_server.controller;

import com.example.ehe_server.service.alert.AlertWebSocketSubscriptionManager;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AlertWebSocketController {

    private final AlertWebSocketSubscriptionManager alertWebSocketSubscriptionManager;
    private final LoggingServiceInterface loggingService;
    private final WebSocketAuthServiceInterface webSocketAuthService;

    public AlertWebSocketController(
            AlertWebSocketSubscriptionManager alertWebSocketSubscriptionManager,
            LoggingServiceInterface loggingService,
            WebSocketAuthServiceInterface webSocketAuthService) {
        this.alertWebSocketSubscriptionManager = alertWebSocketSubscriptionManager;
        this.loggingService = loggingService;
        this.webSocketAuthService = webSocketAuthService;
    }

    @MessageMapping("/alerts/subscribe")
    @SendToUser("/queue/alerts")
    public Map<String, Object> subscribeToAlerts(StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extract user ID from WebSocket authentication
            Integer userId = webSocketAuthService.getUserIdFromWebSocketAuth(headerAccessor);

            if (userId == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return response;
            }

            // Log subscription request
            loggingService.logAction("WebSocket subscription request for alerts from user " + userId);

            // Create subscription for this user
            String subscriptionId = alertWebSocketSubscriptionManager.createSubscription(
                    userId,
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
            @Payload Map<String, String> request,
            StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extract user ID from WebSocket authentication
            Integer userId = webSocketAuthService.getUserIdFromWebSocketAuth(headerAccessor);

            if (userId == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return response;
            }

            String subscriptionId = request.get("subscriptionId");

            if (subscriptionId == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            loggingService.logAction("WebSocket unsubscribe request for alert subscription " + subscriptionId + " from user " + userId);

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