package com.example.ehe_server.controller;

import com.example.ehe_server.service.automatictrade.AutomatedTradeWebSocketSubscriptionManager;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AutomatedTradeWebSocketController {

    private final AutomatedTradeWebSocketSubscriptionManager subscriptionManager;
    private final LoggingServiceInterface loggingService;

    public AutomatedTradeWebSocketController(
            AutomatedTradeWebSocketSubscriptionManager subscriptionManager,
            LoggingServiceInterface loggingService) {
        this.subscriptionManager = subscriptionManager;
        this.loggingService = loggingService;
    }

    @MessageMapping("/automated-trades/subscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> subscribeToAutomatedTrades(
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Extract user ID from JWT token
            Authentication authentication = (Authentication) headerAccessor.getUser();
            Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;

            if (userId == null) {
                response.put("success", false);
                response.put("message", "User authentication required");
                return response;
            }

            // Log subscription request
            loggingService.logAction(
                    userId.intValue(),
                    userId.toString(),
                    "WebSocket subscription request for automated trades");

            // Create subscription for this user
            String subscriptionId = subscriptionManager.createSubscription(
                    userId.intValue(),
                    "/user/queue/automated-trades");

            // Return subscription details
            response.put("success", true);
            response.put("subscriptionId", subscriptionId);
            response.put("message", "Automated trade subscription created successfully");

        } catch (Exception e) {
            loggingService.logError(null, "system",
                    "Error creating automated trade subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error creating automated trade subscription: " + e.getMessage());
        }

        return response;
    }

    @MessageMapping("/automated-trades/unsubscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> unsubscribeFromAutomatedTrades(
            @Payload Map<String, String> request,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        try {
            Authentication authentication = (Authentication) headerAccessor.getUser();
            Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
            String subscriptionId = request.get("subscriptionId");

            if (subscriptionId == null) {
                response.put("success", false);
                response.put("message", "Missing subscription ID");
                return response;
            }

            loggingService.logAction(
                    userId != null ? userId.intValue() : null,
                    userId != null ? userId.toString() : "anonymous",
                    "WebSocket unsubscribe request for automated trade subscription " + subscriptionId);

            boolean cancelled = subscriptionManager.cancelSubscription(subscriptionId);

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
            loggingService.logError(null, "system",
                    "Error cancelling automated trade subscription: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error cancelling automated trade subscription: " + e.getMessage());
        }

        return response;
    }
}