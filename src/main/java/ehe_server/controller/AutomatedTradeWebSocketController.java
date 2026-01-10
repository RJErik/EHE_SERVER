package ehe_server.controller;

import ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import ehe_server.dto.websocket.AutomatedTradeUnsubscriptionRequest;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeWebSocketSubscriptionManagerInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AutomatedTradeWebSocketController {

    private final AutomatedTradeWebSocketSubscriptionManagerInterface automatedTradeWebSocketSubscriptionManager;
    private final UserContextServiceInterface userContextService;
    private final MessageSource messageSource;

    public AutomatedTradeWebSocketController(
            AutomatedTradeWebSocketSubscriptionManagerInterface automatedTradeWebSocketSubscriptionManager,
            UserContextServiceInterface userContextService,
            MessageSource messageSource) {
        this.automatedTradeWebSocketSubscriptionManager = automatedTradeWebSocketSubscriptionManager;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    @MessageMapping("/automated-trades/subscribe")
    @SendToUser("/queue/automated-trades")
    public Map<String, Object> subscribeToAutomatedTrades(
            @Header("simpSessionId")
            String sessionId,
            StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        // Extract user ID from WebSocket authentication
        Integer userId = userContextService.getUserIdFromWebSocketAuth(headerAccessor);

        // Create subscription for this user
        AutomatedTradeSubscriptionResponse automatedTradeSubscriptionResponse = automatedTradeWebSocketSubscriptionManager.createSubscription(
                userId,
                sessionId,
                "/user/queue/automated-trades");

        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.create", // The key from your properties file
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
                "success.message.automatedTradeRule.cancel", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", new AutomatedTradeSubscriptionResponse(subscriptionId));


        return response;
    }
}