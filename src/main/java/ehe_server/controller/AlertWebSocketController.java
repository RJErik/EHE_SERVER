package ehe_server.controller;

import ehe_server.dto.websocket.AlertSubscriptionResponse;
import ehe_server.dto.websocket.AlertUnsubscriptionRequest;
import ehe_server.dto.websocket.AlertUnsubscriptionResponse;
import ehe_server.service.intf.alert.websocket.AlertWebSocketSubscriptionManagerInterface;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import jakarta.validation.Valid;
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
public class AlertWebSocketController {

    private final AlertWebSocketSubscriptionManagerInterface alertWebSocketSubscriptionManager;
    private final UserContextServiceInterface userContextService;
    private final MessageSource messageSource;

    public AlertWebSocketController(
            AlertWebSocketSubscriptionManagerInterface alertWebSocketSubscriptionManager,
            UserContextServiceInterface userContextService,
            MessageSource messageSource) {
        this.alertWebSocketSubscriptionManager = alertWebSocketSubscriptionManager;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    @MessageMapping("/alerts/subscribe")
    @SendToUser("/queue/alerts")
    public Map<String, Object> subscribeToAlerts(
            @Header("simpSessionId")
            String sessionId,
            StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        // Extract user ID from WebSocket authentication
        Integer userId = userContextService.getUserIdFromWebSocketAuth(headerAccessor);

        // Create subscription for this user
        AlertSubscriptionResponse alertSubscriptionResponse = alertWebSocketSubscriptionManager.createSubscription(
                userId,
                sessionId,
                "/user/queue/alerts");

        String successMessage = messageSource.getMessage(
                "success.message.alert.subscription.create", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // Return subscription details
        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", alertSubscriptionResponse);

        return response;
    }

    @MessageMapping("/alerts/unsubscribe")
    @SendToUser("/queue/alerts")
    public Map<String, Object> unsubscribeFromAlerts(
            @Valid @Payload AlertUnsubscriptionRequest request) {

        Map<String, Object> response = new HashMap<>();

        String subscriptionId = request.getSubscriptionId();

        alertWebSocketSubscriptionManager.cancelSubscription(subscriptionId);

        String successMessage = messageSource.getMessage(
                "success.message.alert.subscription.cancel", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        response.put("success", true);
        response.put("message", successMessage);
        response.put("subscriptionId", new AlertUnsubscriptionResponse(subscriptionId));


        return response;
    }
}