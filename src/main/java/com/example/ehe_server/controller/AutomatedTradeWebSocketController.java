package com.example.ehe_server.controller;

import com.example.ehe_server.service.automatictrade.AutomatedTradeWebSocketSubscriptionManager;
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
public class AutomatedTradeWebSocketController {

    private final AutomatedTradeWebSocketSubscriptionManager automatedTradeWebSocketSubscriptionManager;
    private final LoggingServiceInterface loggingService;
    private final WebSocketAuthServiceInterface webSocketAuthService;

    public AutomatedTradeWebSocketController(
            AutomatedTradeWebSocketSubscriptionManager automatedTradeWebSocketSubscriptionManager,
            LoggingServiceInterface loggingService,
            WebSocketAuthServiceInterface webSocketAuthService) {
        this.automatedTradeWebSocketSubscriptionManager = automatedTradeWebSocketSubscriptionManager;
        this.loggingService = loggingService;
        this.webSocketAuthService = webSocketAuthService;
    }

    @MessageMapping("/automated-trades/subscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> subscribeToAutomatedTrades(StompHeaderAccessor headerAccessor) {

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
            loggingService.logAction("WebSocket subscription request for automated trades from user " + userId);

            // Create subscription for this user
            String subscriptionId = automatedTradeWebSocketSubscriptionManager.createSubscription(
                    userId,
                    "/user/queue/automated-trades");

            // Return subscription details
            response.put("success", true);
            response.put("subscriptionId", subscriptionId);
            response.put("message", "Automated trade subscription created successfully");

        } catch (Exception e) {
            loggingService.logError("Error creating automated trade subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error creating automated trade subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/automated-trades/unsubscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> unsubscribeFromAutomatedTrades(
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

            loggingService.logAction("WebSocket unsubscribe request for automated trade subscription " + subscriptionId + " from user " + userId);

            boolean cancelled = automatedTradeWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

            if (cancelled) {
                response.put("success", true);
                response.put("message", "Automated trade subscription cancelled successfully");
                response.put("subscriptionId", subscriptionId);
            } else {
                response.put("success", false);
                response.put("message", "Automated trade subscription not found: " + subscriptionId);
                response.put("subscriptionId", subscriptionId);
            }

        } catch (Exception e) {
            loggingService.logError("Error cancelling automated trade subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error cancelling automated trade subscription: " + e.getMessage());
        }

        return response;
    }
}