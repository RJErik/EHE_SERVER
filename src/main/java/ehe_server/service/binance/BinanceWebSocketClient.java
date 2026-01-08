package ehe_server.service.binance;

import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.binance.BinanceWebSocketClientInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class BinanceWebSocketClient extends TextWebSocketHandler implements BinanceWebSocketClientInterface {
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    // Single connection for all subscriptions
    private volatile WebSocketSession session;

    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    private final Map<String, Consumer<JsonNode>> handlers = new ConcurrentHashMap<>();

    private final AtomicInteger lastId = new AtomicInteger(1);
    private static final String WS_BASE_URL = "wss://stream.binance.com:9443/ws";
    private static final String INTERVAL = "1m";

    public BinanceWebSocketClient(
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextService userContextService) {
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    /**
     * Update subscriptions with a complete list of symbols.
     * This will reconnect the websocket if the symbol list changes.
     */
    public synchronized void updateSubscriptions(List<String> symbols) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        Set<String> normalizedSymbols = symbols.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // If nothing changed, don't reconnect
        if (normalizedSymbols.equals(subscriptions)) {
            loggingService.logAction("Subscription list unchanged, skipping reconnection");
            return;
        }

        loggingService.logAction("Updating subscriptions. Old: " + subscriptions.size() +
                ", New: " + normalizedSymbols.size());

        // Close existing connection
        if (session != null && session.isOpen()) {
            try {
                session.close();
                loggingService.logAction("Closed existing WebSocket connection");
            } catch (Exception e) {
                loggingService.logError("Error closing WebSocket: " + e.getMessage(), e);
            }
        }

        subscriptions.clear();
        subscriptions.addAll(normalizedSymbols);

        // Connect with new symbols if any
        if (!subscriptions.isEmpty()) {
            connect();
        } else {
            loggingService.logAction("No symbols to subscribe to, connection idle");
            session = null;
        }
    }

    /**
     * Add symbols to the existing subscription
     */
    @Override
    public synchronized void addSymbols(List<String> symbols) {
        List<String> updatedList = new ArrayList<>(subscriptions);
        symbols.stream()
                .map(String::toLowerCase)
                .filter(s -> !updatedList.contains(s))
                .forEach(updatedList::add);

        updateSubscriptions(updatedList);
    }

    /**
     * Remove symbols from the subscription
     */
    @Override
    public synchronized void removeSymbols(List<String> symbols) {
        Set<String> toRemove = symbols.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<String> remaining = subscriptions.stream()
                .filter(s -> !toRemove.contains(s))
                .collect(Collectors.toList());

        updateSubscriptions(remaining);
    }

    /**
     * Register a message handler for a specific symbol
     */
    public void registerHandler(String symbol, Consumer<JsonNode> handler) {
        handlers.put(symbol.toLowerCase(), handler);
        loggingService.logAction("Registered handler for symbol: " + symbol);
    }

    /**
     * Unregister a message handler for a specific symbol
     */
    public void unregisterHandler(String symbol) {
        handlers.remove(symbol.toLowerCase());
        loggingService.logAction("Unregistered handler for symbol: " + symbol);
    }

    private void connect() {
        if (subscriptions.isEmpty()) {
            loggingService.logAction("No symbols to subscribe to, skipping connection");
            return;
        }

        userContextService.setUser("SYSTEM", "SYSTEM");

        try {
            WebSocketClient client = new StandardWebSocketClient();
            session = client.doHandshake(this, null, URI.create(WS_BASE_URL)).get();

            TextMessage subscribeMsg = createSubscriptionMessage(new ArrayList<>(subscriptions));
            session.sendMessage(subscribeMsg);

            loggingService.logAction("WebSocket connected. Subscribed to " + subscriptions.size() +
                    " symbols: " + subscriptions);
        } catch (Exception e) {
            loggingService.logError("Failed to connect to Binance WebSocket: " + e.getMessage(), e);
            session = null;
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    /**
     * Creates a subscription message with multiple streams
     * Format: [symbol1@kline_1m, symbol2@kline_1m, ...]
     */
    private TextMessage createSubscriptionMessage(List<String> symbols) {
        String params = symbols.stream()
                .map(symbol -> "\"" + symbol + "@kline_" + INTERVAL + "\"")
                .collect(Collectors.joining(","));

        String subscribePayload = "{" +
                "\"method\": \"SUBSCRIBE\"," +
                "\"params\": [" + params + "]," +
                "\"id\": " + lastId.getAndIncrement() +
                "}";
        return new TextMessage(subscribePayload);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());

            // Handle subscription confirmation responses
            if (jsonNode.has("id") && jsonNode.has("result")) {
                loggingService.logAction("Subscription confirmed for ID: " + jsonNode.get("id"));
                return;
            }

            // Handle kline stream data
            if (jsonNode.has("e") && "kline".equals(jsonNode.get("e").asText())) {
                JsonNode k = jsonNode.get("k");
                String symbol = jsonNode.get("s").asText().toLowerCase();

                Consumer<JsonNode> handler = handlers.get(symbol);
                if (handler != null) {
                    handler.accept(jsonNode);
                } else {
                    loggingService.logAction("No handler registered for symbol: " + symbol);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error processing WebSocket message: " + e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance WebSocket connection established with " +
                subscriptions.size() + " subscriptions");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logError("Binance WebSocket transport error: " + exception.getMessage(), exception);

        // Schedule reconnection attempt
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                loggingService.logAction("Attempting to reconnect after transport error");
                synchronized (this) {
                    if (session.equals(BinanceWebSocketClient.this.session)) {
                        connect();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance WebSocket connection closed: " + status);

        // Attempt reconnect if we still have subscriptions
        if (!subscriptions.isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    loggingService.logAction("Attempting to reconnect with " + subscriptions.size() + " symbols");
                    synchronized (this) {
                        if (BinanceWebSocketClient.this.session == null ||
                                !BinanceWebSocketClient.this.session.isOpen()) {
                            connect();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Check if WebSocket is currently connected
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Get current subscription count
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }

    /**
     * Get list of currently subscribed symbols
     */
    public Set<String> getSubscribedSymbols() {
        return new HashSet<>(subscriptions);
    }
}