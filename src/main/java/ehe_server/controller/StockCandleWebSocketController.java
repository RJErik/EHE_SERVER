package ehe_server.controller;

import com.example.ehe_server.dto.websocket.*;
import ehe_server.dto.websocket.StockCandleSubscriptionRequest;
import ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import ehe_server.dto.websocket.StockCandleUnsubscriptionRequest;
import ehe_server.dto.websocket.StockCandleUnsubscriptionResponse;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.stock.websocket.StockWebSocketSubscriptionManagerInterface;
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
public class StockCandleWebSocketController {

    private final StockWebSocketSubscriptionManagerInterface stockWebSocketSubscriptionManager;
    private final UserContextServiceInterface userContextService;
    private final MessageSource messageSource;

    public StockCandleWebSocketController(
            StockWebSocketSubscriptionManagerInterface stockWebSocketSubscriptionManager,
            UserContextServiceInterface userContextService,
            MessageSource messageSource) {
        this.stockWebSocketSubscriptionManager = stockWebSocketSubscriptionManager;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    @MessageMapping("/candles/subscribe")
    @SendToUser("/queue/candles")
    public Map<String, Object> subscribeToCandles(
            @Payload StockCandleSubscriptionRequest request,
            @Header("simpSessionId") String sessionId,
            StompHeaderAccessor headerAccessor) {

        Map<String, Object> response = new HashMap<>();

        // Extract user ID from WebSocket authentication
        Integer userId = userContextService.getUserIdFromWebSocketAuth(headerAccessor);

        // Create subscription
        StockCandleSubscriptionResponse stockCandleSubscriptionResponse = stockWebSocketSubscriptionManager.createSubscription(
                userId,
                sessionId,
                request.getPlatformName(),
                request.getStockSymbol(),
                request.getTimeframe(),
                "/user/" + userId + "/queue/candles");

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
}