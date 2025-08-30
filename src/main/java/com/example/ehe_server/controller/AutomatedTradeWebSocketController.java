package com.example.ehe_server.controller;

import com.example.ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import com.example.ehe_server.dto.websocket.AutomatedTradeUnsubscriptionRequest;
import com.example.ehe_server.service.automatictrade.AutomatedTradeWebSocketSubscriptionManager;
import com.example.ehe_server.service.intf.audit.WebSocketAuthServiceInterface;
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
public class AutomatedTradeWebSocketController {

    private final AutomatedTradeWebSocketSubscriptionManager automatedTradeWebSocketSubscriptionManager;
    private final WebSocketAuthServiceInterface webSocketAuthService;
    private final MessageSource messageSource;

    public AutomatedTradeWebSocketController(
            AutomatedTradeWebSocketSubscriptionManager automatedTradeWebSocketSubscriptionManager,
            WebSocketAuthServiceInterface webSocketAuthService,
            MessageSource messageSource) {
        this.automatedTradeWebSocketSubscriptionManager = automatedTradeWebSocketSubscriptionManager;
        this.webSocketAuthService = webSocketAuthService;
        this.messageSource = messageSource;
    }

    @MessageMapping("/automated-trades/subscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> subscribeToAutomatedTrades(StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        // Extract user ID from WebSocket authentication
        Integer userId = webSocketAuthService.getUserIdFromWebSocketAuth(headerAccessor);

        // Create subscription for this user
        AutomatedTradeSubscriptionResponse automatedTradeSubscriptionResponse = automatedTradeWebSocketSubscriptionManager.createSubscription(
                userId,
                "/user/queue/automated-trades");

        String successMessage = messageSource.getMessage(
                "success.message.alert.automatedTradeRule.create", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // Return subscription details
        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", automatedTradeSubscriptionResponse);


        return response;
    }

    @MessageMapping("/automated-trades/unsubscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> unsubscribeFromAutomatedTrades(
            @Payload AutomatedTradeUnsubscriptionRequest request) {

        Map<String, Object> response = new HashMap<>();

        String subscriptionId = request.getSubscriptionId();

        automatedTradeWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

        String successMessage = messageSource.getMessage(
                "success.message.alert.automatedTradeRule.cancel", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", new AutomatedTradeSubscriptionResponse(subscriptionId));


        return response;
    }
}