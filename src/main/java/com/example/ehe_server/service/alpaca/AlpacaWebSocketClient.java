package com.example.ehe_server.service.alpaca;

import com.example.ehe_server.securityConfig.AlpacaProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AlpacaWebSocketClient extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final AlpacaProperties alpacaProperties;

    // WebSocket connection
    private volatile WebSocketSession session;
    private volatile boolean authenticated = false;

    // Track subscribed symbols
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();

    // Handlers for each symbol
    private final Map<String, Consumer<JsonNode>> handlers = new ConcurrentHashMap<>();

    // Determine which feed to use based on symbol type
    private static final String STOCK_FEED_PATH = "/v2/iex"; // or /v2/sip for paid plans
    private static final String CRYPTO_FEED_PATH = "/v1beta3/crypto";

    public AlpacaWebSocketClient(
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            AlpacaProperties alpacaProperties) {
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.alpacaProperties = alpacaProperties;
    }

    /**
     * Update subscriptions with a complete list of symbols
     * This will reconnect the websocket if the symbol list changes
     */
    public synchronized void updateSubscriptions(List<String> symbols) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        Set<String> normalizedSymbols = symbols.stream()
                .map(String::toUpperCase)
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
        authenticated = false;

        // Connect with new symbols if any
        if (!subscriptions.isEmpty()) {
            // Separate stocks and crypto
            List<String> stocks = new ArrayList<>();
            List<String> cryptos = new ArrayList<>();

            for (String symbol : subscriptions) {
                if (isCryptoSymbol(symbol)) {
                    cryptos.add(symbol);
                } else {
                    stocks.add(symbol);
                }
            }

            // Connect to appropriate feeds
            if (!stocks.isEmpty()) {
                connectToFeed(stocks, STOCK_FEED_PATH);
            }
            if (!cryptos.isEmpty()) {
                connectToFeed(cryptos, CRYPTO_FEED_PATH);
            }
        } else {
            loggingService.logAction("No symbols to subscribe to, connection idle");
            session = null;
        }
    }

    /**
     * Connect to a specific Alpaca data feed
     */
    private void connectToFeed(List<String> symbols, String feedPath) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        try {
            String wsUrl = alpacaProperties.getWebsocketurl() + feedPath;

            WebSocketClient client = new StandardWebSocketClient();
            session = client.doHandshake(this, null, URI.create(wsUrl)).get();

            loggingService.logAction("WebSocket connected to " + feedPath +
                    ". Awaiting authentication...");

            // Authentication will happen in afterConnectionEstablished()
            // Subscription will happen after auth confirmation

        } catch (Exception e) {
            loggingService.logError("Failed to connect to Alpaca WebSocket: " + e.getMessage(), e);
            session = null;
            authenticated = false;
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    /**
     * Register a message handler for a specific symbol
     */
    public void registerHandler(String symbol, Consumer<JsonNode> handler) {
        handlers.put(symbol.toUpperCase(), handler);
        loggingService.logAction("Registered handler for symbol: " + symbol);
    }

    /**
     * Unregister a message handler for a specific symbol
     */
    public void unregisterHandler(String symbol) {
        handlers.remove(symbol.toUpperCase());
        loggingService.logAction("Unregistered handler for symbol: " + symbol);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Alpaca WebSocket connection established. Sending authentication...");

        try {
            // Send authentication message
            TextMessage authMsg = createAuthMessage();
            session.sendMessage(authMsg);
        } catch (IOException e) {
            loggingService.logError("Failed to send auth message: " + e.getMessage(), e);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        try {
            JsonNode jsonArray = objectMapper.readTree(message.getPayload());

            // Alpaca sends messages as an array
            if (!jsonArray.isArray()) {
                loggingService.logAction("Received non-array message: " + message.getPayload());
                return;
            }

            for (JsonNode jsonNode : jsonArray) {
                String messageType = jsonNode.has("T") ? jsonNode.get("T").asText() : "";

                switch (messageType) {
                    case "success":
                        handleSuccessMessage(jsonNode);
                        break;
                    case "error":
                        handleErrorMessage(jsonNode);
                        break;
                    case "subscription":
                        handleSubscriptionMessage(jsonNode);
                        break;
                    case "b": // bar/candle
                        handleBarMessage(jsonNode);
                        break;
                    case "t": // trade
                    case "q": // quote
                        // Handle if needed
                        break;
                    default:
                        loggingService.logAction("Unknown message type: " + messageType);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error processing WebSocket message: " + e.getMessage(), e);
        }
    }

    private void handleSuccessMessage(JsonNode jsonNode) {
        String msg = jsonNode.get("msg").asText();
        loggingService.logAction("WebSocket success: " + msg);

        if (msg.contains("authenticated")) {
            authenticated = true;
            loggingService.logAction("Authentication successful");

            // Now send subscription message
            try {
                TextMessage subscribeMsg = createSubscriptionMessage(new ArrayList<>(subscriptions));
                session.sendMessage(subscribeMsg);
            } catch (IOException e) {
                loggingService.logError("Failed to send subscription message: " + e.getMessage(), e);
            }
        }
    }

    private void handleErrorMessage(JsonNode jsonNode) {
        int code = jsonNode.get("code").asInt();
        String msg = jsonNode.get("msg").asText();
        loggingService.logError("WebSocket error (code " + code + "): " + msg, null);
    }

    private void handleSubscriptionMessage(JsonNode jsonNode) {
        loggingService.logAction("Subscription confirmed: " + jsonNode.toString());
    }

    private void handleBarMessage(JsonNode jsonNode) {
        String symbol = jsonNode.get("S").asText();

        Consumer<JsonNode> handler = handlers.get(symbol);
        if (handler != null) {
            handler.accept(jsonNode);
        } else {
            loggingService.logAction("No handler registered for symbol: " + symbol);
        }
    }

    /**
     * Creates authentication message for Alpaca WebSocket
     * Format: {"action":"auth","key":"YOUR_KEY","secret":"YOUR_SECRET"}
     */
    private TextMessage createAuthMessage() throws IOException {
        Map<String, String> authPayload = new HashMap<>();
        authPayload.put("action", "auth");
        authPayload.put("key", alpacaProperties.getApikey());
        authPayload.put("secret", alpacaProperties.getSecret());

        String json = objectMapper.writeValueAsString(authPayload);
        return new TextMessage(json);
    }

    /**
     * Creates subscription message for Alpaca WebSocket
     * Format: {"action":"subscribe","bars":["AAPL","GOOGL"]}
     */
    private TextMessage createSubscriptionMessage(List<String> symbols) throws IOException {
        Map<String, Object> subscribePayload = new HashMap<>();
        subscribePayload.put("action", "subscribe");
        subscribePayload.put("bars", symbols);
        // Can also subscribe to trades and quotes if needed
        // subscribePayload.put("trades", symbols);
        // subscribePayload.put("quotes", symbols);

        String json = objectMapper.writeValueAsString(subscribePayload);
        loggingService.logAction("Sending subscription for " + symbols.size() + " symbols");
        return new TextMessage(json);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logError("Alpaca WebSocket transport error: " + exception.getMessage(), exception);

        // Schedule reconnection attempt
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                loggingService.logAction("Attempting to reconnect after transport error");
                synchronized (this) {
                    if (!subscriptions.isEmpty()) {
                        updateSubscriptions(new ArrayList<>(subscriptions));
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
        loggingService.logAction("Alpaca WebSocket connection closed: " + status);
        authenticated = false;

        // Attempt reconnect if we still have subscriptions
        if (!subscriptions.isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    loggingService.logAction("Attempting to reconnect with " + subscriptions.size() + " symbols");
                    synchronized (this) {
                        updateSubscriptions(new ArrayList<>(subscriptions));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Check if WebSocket is currently connected and authenticated
     */
    public boolean isConnected() {
        return session != null && session.isOpen() && authenticated;
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

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}