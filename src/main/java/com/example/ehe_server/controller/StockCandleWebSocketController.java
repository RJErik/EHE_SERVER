package com.example.ehe_server.controller;

import com.example.ehe_server.dto.websocket.CandleSubscriptionRequest;
import com.example.ehe_server.dto.websocket.SubscriptionUpdateRequest;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.stock.WebSocketSubscriptionManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StockCandleWebSocketController {

    private final WebSocketSubscriptionManager subscriptionManager;
    private final LoggingServiceInterface loggingService;

    public StockCandleWebSocketController(
            WebSocketSubscriptionManager subscriptionManager,
            LoggingServiceInterface loggingService) {
        this.subscriptionManager = subscriptionManager;
        this.loggingService = loggingService;
    }

    @MessageMapping("/candles/subscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> subscribeToCandles(
            @Payload CandleSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get user information from authentication
            Authentication authentication = (Authentication) headerAccessor.getUser();
            Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;

            // Log subscription request
            loggingService.logAction(
                    userId != null ? userId.intValue() : null,
                    userId != null ? userId.toString() : "anonymous",
                    "WebSocket subscription request for " +
                            request.getPlatformName() + ":" + request.getStockSymbol() +
                            " " + request.getTimeframe() +
                            (request.getSubscriptionType() != null ? " (Type: " + request.getSubscriptionType() + ")" : ""));

            // Validate request
            if (request.getPlatformName() == null || request.getStockSymbol() == null ||
                    request.getTimeframe() == null || request.getStartDate() == null ||
                    request.getEndDate() == null) {

                response.put("success", false);
                response.put("message", "Missing required parameters");
                return response;
            }

            // Create subscription
            String subscriptionId = subscriptionManager.createSubscription(
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
            loggingService.logError(null, "system",
                    "Error creating subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error creating subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/candles/unsubscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> unsubscribeFromCandles(
            @Payload Map<String, String> request,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get user information
            Authentication authentication = (Authentication) headerAccessor.getUser();
            Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;

            // Get subscription ID
            String subscriptionId = request.get("subscriptionId");

            if (subscriptionId == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            // Log unsubscribe request
            loggingService.logAction(
                    userId != null ? userId.intValue() : null,
                    userId != null ? userId.toString() : "anonymous",
                    "WebSocket unsubscribe request for subscription " + subscriptionId);

            // Cancel subscription
            boolean cancelled = subscriptionManager.cancelSubscription(subscriptionId);

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
            loggingService.logError(null, "system",
                    "Error cancelling subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error cancelling subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/candles/update-subscription")
    @SendToUser("/queue/candles")
    public Map<String, Object> updateSubscription(
            @Payload SubscriptionUpdateRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get user information
            Authentication authentication = (Authentication) headerAccessor.getUser();
            Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;

            // Validate request
            if (request.getSubscriptionId() == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            // Log update request
            loggingService.logAction(
                    userId != null ? userId.intValue() : null,
                    userId != null ? userId.toString() : "anonymous",
                    "WebSocket subscription update request for " + request.getSubscriptionId() +
                            " with new date range: " +
                            (request.getNewStartDate() != null ? request.getNewStartDate() : "unchanged") + " to " +
                            (request.getNewEndDate() != null ? request.getNewEndDate() : "unchanged"));

            // Update subscription
            boolean updated = subscriptionManager.updateSubscription(
                    request.getSubscriptionId(),
                    request.getNewStartDate(),
                    request.getNewEndDate(),
                    request.getResetData() != null && request.getResetData());

            if (updated) {
                response.put("success", true);
                response.put("message", "Subscription updated successfully");
                response.put("subscriptionId", request.getSubscriptionId());
            } else {
                response.put("success", false);
                response.put("message", "Subscription not found: " + request.getSubscriptionId());
                response.put("subscriptionId", request.getSubscriptionId());
            }

        } catch (Exception e) {
            loggingService.logError(null, "system",
                    "Error updating subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error updating subscription: " + e.getMessage());
        }

        return response;
    }
}
