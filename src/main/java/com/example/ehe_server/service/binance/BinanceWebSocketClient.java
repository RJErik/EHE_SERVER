package com.example.ehe_server.service.binance;

import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@Transactional
public class BinanceWebSocketClient extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> handlers = new ConcurrentHashMap<>();
    private final AtomicInteger lastId = new AtomicInteger(1);

    private static final String WS_BASE_URL = "wss://stream.binance.com:9443/ws";
    private final UserContextService userContextService;

    public BinanceWebSocketClient(ObjectMapper objectMapper, LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    public void subscribeToKlineStream(String symbol, String interval, Consumer<JsonNode> handler) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        String streamName = symbol.toLowerCase() + "@kline_" + interval;

        try {
            WebSocketClient client = new StandardWebSocketClient();
            WebSocketSession session = client.doHandshake(this, null, URI.create(WS_BASE_URL)).get();

            // Store session for management
            sessions.put(streamName, session);
            handlers.put(streamName, handler);

            // Send subscription message
            TextMessage subscribeMsg = createSubscriptionMessage(streamName);
            session.sendMessage(subscribeMsg);

            loggingService.logAction("Subscribed to Binance WebSocket stream: " + streamName);
        } catch (Exception e) {
            loggingService.logError("Failed to connect to Binance WebSocket: " + e.getMessage(), e);
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    private TextMessage createSubscriptionMessage(String streamName) throws IOException {
        String subscribePayload = "{" +
                "\"method\": \"SUBSCRIBE\"," +
                "\"params\": [\"" + streamName + "\"]," +
                "\"id\": " + lastId.getAndIncrement() +
                "}";
        return new TextMessage(subscribePayload);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());

            // Handle subscription responses
            if (jsonNode.has("id") && jsonNode.has("result")) {
                loggingService.logAction("Subscription response: " + message.getPayload());
                return;
            }

            // Handle stream data
            if (jsonNode.has("e") && "kline".equals(jsonNode.get("e").asText())) {
                // Extract symbol and interval
                JsonNode k = jsonNode.get("k");
                String symbol = jsonNode.get("s").asText().toLowerCase();
                String interval = k.get("i").asText();
                String streamName = symbol + "@kline_" + interval;

                Consumer<JsonNode> handler = handlers.get(streamName);
                if (handler != null) {
                    handler.accept(jsonNode);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error processing WebSocket message: " + e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance WebSocket connection established");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logError("Binance WebSocket transport error: " + exception.getMessage(), exception);

        // Attempt to reconnect streams for this session
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                String streamName = entry.getKey();
                Consumer<JsonNode> handler = handlers.get(streamName);

                if (handler != null) {
                    loggingService.logAction("Attempting to reconnect stream: " + streamName);
                    sessions.remove(streamName);

                    // Extract symbol and interval from stream name
                    String[] parts = streamName.split("@kline_");
                    if (parts.length == 2) {
                        String symbol = parts[0];
                        String interval = parts[1];
                        subscribeToKlineStream(symbol, interval, handler);
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance WebSocket connection closed: " + status);

        // Similar reconnection logic as in handleTransportError
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                // Schedule reconnection
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds before reconnecting
                        handleTransportError(session, new Exception("Connection closed: " + status));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                break;
            }
        }
    }
}
