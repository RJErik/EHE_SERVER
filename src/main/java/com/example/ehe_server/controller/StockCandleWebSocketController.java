package com.example.ehe_server.controller;

import com.example.ehe_server.dto.websocket.*;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
import com.example.ehe_server.service.stock.StockWebSocketSubscriptionManager;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final WebSocketAuthServiceInterface webSocketAuthService;
    private final MessageSource messageSource;

    public StockCandleWebSocketController(
            StockWebSocketSubscriptionManager stockWebSocketSubscriptionManager,
            WebSocketAuthServiceInterface webSocketAuthService,
            MessageSource messageSource) {
        this.stockWebSocketSubscriptionManager = stockWebSocketSubscriptionManager;
        this.webSocketAuthService = webSocketAuthService;
        this.messageSource = messageSource;
    }

    @MessageMapping("/candles/subscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> subscribeToCandles(
            @Payload StockCandleSubscriptionRequest request,
            StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        // Extract user ID from WebSocket authentication
        Integer userId = webSocketAuthService.getUserIdFromWebSocketAuth(headerAccessor);

        // Create subscription
        StockCandleSubscriptionResponse stockCandleSubscriptionResponse = stockWebSocketSubscriptionManager.createSubscription(
                request.getPlatformName(),
                request.getStockSymbol(),
                request.getTimeframe(),
                request.getStartDate(),
                request.getEndDate(),
                "/user/" + userId + "/queue/candles",
                request.getSubscriptionType());

        String successMessage = messageSource.getMessage(
                "success.message.stock.candle.create", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        //LOOK OVER FRONTEND

        // Return subscription details
        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscription", stockCandleSubscriptionResponse);

        return response;
    }

    @MessageMapping("/candles/unsubscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> unsubscribeFromCandles(
            @Payload StockCandleUnsubscriptionRequest request) {

        Map<String, Object> response = new HashMap<>();

        // Get subscription ID
        String subscriptionId = request.getSubscriptionId();

        // Cancel subscription
        stockWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

        String successMessage = messageSource.getMessage(
                "success.message.stock.candle.cancel", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", new StockCandleUnsubscriptionResponse(subscriptionId));

        return response;
    }

    @MessageMapping("/candles/update-subscription")
    @SendToUser("/queue/candles")
    public Map<String, Object> updateSubscription(
            @Payload SubscriptionUpdateSubscriptionRequest request) {

        Map<String, Object> response = new HashMap<>();

        // Update subscription with the new type
        StockCandleUpdateSubscriptionResponse stockCandleUpdateSubscriptionResponse = stockWebSocketSubscriptionManager.updateSubscription(
                request.getSubscriptionId(),
                request.getNewStartDate(),
                request.getNewEndDate(),
                request.getResetData() != null && request.getResetData(),
                request.getSubscriptionType());

        String successMessage = messageSource.getMessage(
                "success.message.stock.candle.update", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        //LOOK OVER FRONTEND

        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscription", stockCandleUpdateSubscriptionResponse);

        return response;
    }
}