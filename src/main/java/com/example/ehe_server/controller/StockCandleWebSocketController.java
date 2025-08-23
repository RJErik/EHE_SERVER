package com.example.ehe_server.controller;

import com.example.ehe_server.dto.websocket.CandleSubscriptionRequest;
import com.example.ehe_server.dto.websocket.SubscriptionUpdateRequest;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
import com.example.ehe_server.service.stock.StockWebSocketSubscriptionManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StockCandleWebSocketController {

    private final StockWebSocketSubscriptionManager stockWebSocketSubscriptionManager;
    private final LoggingServiceInterface loggingService;
    private final WebSocketAuthServiceInterface webSocketAuthService;

    public StockCandleWebSocketController(
            StockWebSocketSubscriptionManager stockWebSocketSubscriptionManager,
            LoggingServiceInterface loggingService,
            WebSocketAuthServiceInterface webSocketAuthService) {
        this.stockWebSocketSubscriptionManager = stockWebSocketSubscriptionManager;
        this.loggingService = loggingService;
        this.webSocketAuthService = webSocketAuthService;
    }

    @MessageMapping("/candles/subscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> subscribeToCandles(
            @Payload CandleSubscriptionRequest request,
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

            // Log subscription request
            loggingService.logAction("WebSocket subscription request for " +
                    request.getPlatformName() + ":" + request.getStockSymbol() +
                    " " + request.getTimeframe() +
                    (request.getSubscriptionType() != null ? " (Type: " + request.getSubscriptionType() + ")" : "") +
                    " from user " + userId);

            // Validate request
            if (request.getPlatformName() == null || request.getStockSymbol() == null ||
                    request.getTimeframe() == null || request.getStartDate() == null ||
                    request.getEndDate() == null) {

                response.put("success", false);
                response.put("message", "Missing required parameters");
                return response;
            }

            // Create subscription
            String subscriptionId = stockWebSocketSubscriptionManager.createSubscription(
                    request.getPlatformName(),
                    request.getStockSymbol(),
                    request.getTimeframe(),
                    request.getStartDate(),
                    request.getEndDate(),
                    "/user/" + userId + "/queue/candles",
                    request.getSubscriptionType());

            // Return subscription details
            response.put("success", true);
            response.put("subscriptionId", subscriptionId);
            response.put("message", "Subscription created successfully");
            response.put("subscriptionType", request.getSubscriptionType());

        } catch (Exception e) {
            loggingService.logError("Error creating subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error creating subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/candles/unsubscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> unsubscribeFromCandles(
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

            // Get subscription ID
            String subscriptionId = request.get("subscriptionId");

            if (subscriptionId == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            // Log unsubscribe request
            loggingService.logAction("WebSocket unsubscribe request for subscription " + subscriptionId + " from user " + userId);

            // Cancel subscription
            boolean cancelled = stockWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

            if (cancelled) {
                response.put("success", true);
                response.put("message", "Subscription cancelled successfully");
                response.put("subscriptionId", subscriptionId);
            } else {
                response.put("success", false);
                response.put("message", "Subscription not found: " + subscriptionId);
                response.put("subscriptionId", subscriptionId);
            }

        } catch (Exception e) {
            loggingService.logError("Error cancelling subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error cancelling subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/candles/update-subscription")
    @SendToUser("/queue/candles")
    public Map<String, Object> updateSubscription(
            @Payload SubscriptionUpdateRequest request,
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

            // Validate request
            if (request.getSubscriptionId() == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            // Log update request
            loggingService.logAction("WebSocket subscription update request for " + request.getSubscriptionId() +
                    " with new date range: " +
                    (request.getNewStartDate() != null ? request.getNewStartDate() : "unchanged") + " to " +
                    (request.getNewEndDate() != null ? request.getNewEndDate() : "unchanged") +
                    (request.getSubscriptionType() != null ? " and type: " + request.getSubscriptionType() : "") +
                    " from user " + userId);

            // Update subscription with the new type
            boolean updated = stockWebSocketSubscriptionManager.updateSubscription(
                    request.getSubscriptionId(),
                    request.getNewStartDate(),
                    request.getNewEndDate(),
                    request.getResetData() != null && request.getResetData(),
                    request.getSubscriptionType());

            if (updated) {
                response.put("success", true);
                response.put("message", "Subscription updated successfully");
                response.put("subscriptionId", request.getSubscriptionId());

                // Include the subscription type in the response
                String subscriptionType = stockWebSocketSubscriptionManager.getSubscriptionType(request.getSubscriptionId());
                if (subscriptionType != null) {
                    response.put("subscriptionType", subscriptionType);
                }
            } else {
                response.put("success", false);
                response.put("message", "Subscription not found: " + request.getSubscriptionId());
                response.put("subscriptionId", request.getSubscriptionId());
            }

        } catch (Exception e) {
            loggingService.logError("Error updating subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error updating subscription: " + e.getMessage());
        }

        return response;
    }
}