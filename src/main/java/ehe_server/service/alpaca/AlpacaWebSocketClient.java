package ehe_server.service.alpaca;

import ehe_server.properties.AlpacaProperties;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.alpaca.AlpacaWebSocketClientInterface;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
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

@Service
public class AlpacaWebSocketClient extends TextWebSocketHandler implements AlpacaWebSocketClientInterface {

    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextServiceInterface userContextService;
    private final AlpacaProperties alpacaProperties;

    // Separate sessions for stock and crypto feeds
    private volatile WebSocketSession stockSession;
    private volatile WebSocketSession cryptoSession;

    // Track authentication state per feed
    private volatile boolean stockAuthenticated = false;
    private volatile boolean cryptoAuthenticated = false;

    // Track subscriptions per feed type
    private final Set<String> stockSubscriptions = ConcurrentHashMap.newKeySet();
    private final Set<String> cryptoSubscriptions = ConcurrentHashMap.newKeySet();

    private final Map<String, Consumer<JsonNode>> handlers = new ConcurrentHashMap<>();

    // Map sessions to their feed type for message handling
    private final Map<WebSocketSession, FeedType> sessionFeedTypeMap = new ConcurrentHashMap<>();

    private static final String STOCK_FEED_PATH = "/v2/iex"; // or /v2/sip for paid plans
    private static final String CRYPTO_FEED_PATH = "/v1beta3/crypto/us";

    private enum FeedType {
        STOCK,
        CRYPTO
    }

    public AlpacaWebSocketClient(
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextServiceInterface userContextService,
            AlpacaProperties alpacaProperties) {
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.alpacaProperties = alpacaProperties;
    }

    /**
     * Update subscriptions with a complete list of symbols
     * This will reconnect the websockets if the symbol list changes
     */
    @Override
    public synchronized void updateSubscriptions(List<String> symbols) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        // Separate symbols into stocks and crypto
        Set<String> newStockSymbols = new HashSet<>();
        Set<String> newCryptoSymbols = new HashSet<>();

        for (String symbol : symbols) {
            String normalized = symbol.toUpperCase();
            if (isCryptoSymbol(normalized)) {
                newCryptoSymbols.add(normalized);
            } else {
                newStockSymbols.add(normalized);
            }
        }

        // Update stock subscriptions if changed
        if (!newStockSymbols.equals(stockSubscriptions)) {
            loggingService.logAction("Updating stock subscriptions. Old: " + stockSubscriptions.size() +
                    ", New: " + newStockSymbols.size());

            closeSession(stockSession, "stock");
            stockSession = null;
            stockAuthenticated = false;

            stockSubscriptions.clear();
            stockSubscriptions.addAll(newStockSymbols);

            if (!stockSubscriptions.isEmpty()) {
                connectToFeed(FeedType.STOCK);
            } else {
                loggingService.logAction("No stock symbols to subscribe to");
            }
        } else {
            loggingService.logAction("Stock subscription list unchanged, skipping reconnection");
        }

        // Update crypto subscriptions if changed
        if (!newCryptoSymbols.equals(cryptoSubscriptions)) {
            loggingService.logAction("Updating crypto subscriptions. Old: " + cryptoSubscriptions.size() +
                    ", New: " + newCryptoSymbols.size());

            closeSession(cryptoSession, "crypto");
            cryptoSession = null;
            cryptoAuthenticated = false;

            cryptoSubscriptions.clear();
            cryptoSubscriptions.addAll(newCryptoSymbols);

            if (!cryptoSubscriptions.isEmpty()) {
                connectToFeed(FeedType.CRYPTO);
            } else {
                loggingService.logAction("No crypto symbols to subscribe to");
            }
        } else {
            loggingService.logAction("Crypto subscription list unchanged, skipping reconnection");
        }
    }

    /**
     * Closes a WebSocket session safely
     */
    private void closeSession(WebSocketSession session, String feedName) {
        if (session != null && session.isOpen()) {
            try {
                sessionFeedTypeMap.remove(session);
                session.close();
                loggingService.logAction("Closed existing " + feedName + " WebSocket connection");
            } catch (Exception e) {
                loggingService.logError("Error closing " + feedName + " WebSocket: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Connect to a specific Alpaca data feed
     */
    private void connectToFeed(FeedType feedType) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        String feedPath = feedType == FeedType.STOCK ? STOCK_FEED_PATH : CRYPTO_FEED_PATH;
        String feedName = feedType == FeedType.STOCK ? "stock" : "crypto";

        try {
            String wsUrl = alpacaProperties.getWebsocketurl() + feedPath;

            WebSocketClient client = new StandardWebSocketClient();

            WebSocketSession newSession = client.execute(this, new WebSocketHttpHeaders(), URI.create(wsUrl)).get();

            // Store session and map it to feed type
            sessionFeedTypeMap.put(newSession, feedType);

            if (feedType == FeedType.STOCK) {
                stockSession = newSession;
            } else {
                cryptoSession = newSession;
            }

            loggingService.logAction("WebSocket connected to " + feedName + " feed (" + feedPath +
                    "). Awaiting authentication...");

        } catch (Exception e) {
            loggingService.logError("Failed to connect to Alpaca " + feedName + " WebSocket: " + e.getMessage(), e);

            if (feedType == FeedType.STOCK) {
                stockSession = null;
                stockAuthenticated = false;
            } else {
                cryptoSession = null;
                cryptoAuthenticated = false;
            }

            throw new RuntimeException(feedName + " WebSocket connection failed", e);
        }
    }

    /**
     * Register a message handler for a specific symbol
     */
    @Override
    public void registerHandler(String symbol, Consumer<JsonNode> handler) {
        handlers.put(symbol.toUpperCase(), handler);
        loggingService.logAction("Registered handler for symbol: " + symbol);
    }

    /**
     * Unregister a message handler for a specific symbol
     */
    @Override
    public void unregisterHandler(String symbol) {
        handlers.remove(symbol.toUpperCase());
        loggingService.logAction("Unregistered handler for symbol: " + symbol);
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        FeedType feedType = sessionFeedTypeMap.get(session);
        String feedName = feedType == FeedType.STOCK ? "stock" : "crypto";

        loggingService.logAction("Alpaca " + feedName + " WebSocket connection established. Sending authentication...");

        try {
            // Send authentication message
            TextMessage authMsg = createAuthMessage();
            session.sendMessage(authMsg);
        } catch (IOException e) {
            loggingService.logError("Failed to send auth message to " + feedName + " feed: " + e.getMessage(), e);
        }
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        FeedType feedType = sessionFeedTypeMap.get(session);
        if (feedType == null) {
            loggingService.logError("Received message from unknown session", null);
            return;
        }

        String feedName = feedType == FeedType.STOCK ? "stock" : "crypto";

        try {
            JsonNode jsonArray = objectMapper.readTree(message.getPayload());

            // Alpaca sends messages as an array
            if (!jsonArray.isArray()) {
                loggingService.logAction("Received non-array message from " + feedName + " feed: " + message.getPayload());
                return;
            }

            for (JsonNode jsonNode : jsonArray) {
                String messageType = jsonNode.has("T") ? jsonNode.get("T").asText() : "";

                switch (messageType) {
                    case "success":
                        handleSuccessMessage(jsonNode, session, feedType);
                        break;
                    case "error":
                        handleErrorMessage(jsonNode, feedName);
                        break;
                    case "subscription":
                        handleSubscriptionMessage(jsonNode, feedName);
                        break;
                    case "b": // bar/candle
                        handleBarMessage(jsonNode);
                        break;
                    case "t": // trade
                    case "q": // quote
                        // Handle if needed
                        break;
                    default:
                        loggingService.logAction("Unknown message type from " + feedName + " feed: " + messageType);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error processing " + feedName + " WebSocket message: " + e.getMessage(), e);
        }
    }

    private void handleSuccessMessage(JsonNode jsonNode, WebSocketSession session, FeedType feedType) {
        String msg = jsonNode.get("msg").asText();
        String feedName = feedType == FeedType.STOCK ? "stock" : "crypto";

        loggingService.logAction(feedName + " WebSocket success: " + msg);

        if (msg.contains("authenticated")) {
            // Set authenticated flag for the appropriate feed
            if (feedType == FeedType.STOCK) {
                stockAuthenticated = true;
            } else {
                cryptoAuthenticated = true;
            }

            loggingService.logAction(feedName + " feed authentication successful");

            // Now send subscription message for the appropriate symbols
            try {
                Set<String> symbolsToSubscribe = feedType == FeedType.STOCK ? stockSubscriptions : cryptoSubscriptions;
                TextMessage subscribeMsg = createSubscriptionMessage(new ArrayList<>(symbolsToSubscribe));
                session.sendMessage(subscribeMsg);
            } catch (IOException e) {
                loggingService.logError("Failed to send subscription message to " + feedName + " feed: " + e.getMessage(), e);
            }
        }
    }

    private void handleErrorMessage(JsonNode jsonNode, String feedName) {
        int code = jsonNode.get("code").asInt();
        String msg = jsonNode.get("msg").asText();
        loggingService.logError(feedName + " WebSocket error (code " + code + "): " + msg, null);
    }

    private void handleSubscriptionMessage(JsonNode jsonNode, String feedName) {
        loggingService.logAction(feedName + " subscription confirmed: " + jsonNode.toString());
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

        String json = objectMapper.writeValueAsString(subscribePayload);
        loggingService.logAction("Sending subscription for " + symbols.size() + " symbols: " + symbols);
        return new TextMessage(json);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        FeedType feedType = sessionFeedTypeMap.get(session);
        String feedName = feedType != null ? (feedType == FeedType.STOCK ? "stock" : "crypto") : "unknown";

        loggingService.logError("Alpaca " + feedName + " WebSocket transport error: " + exception.getMessage(), exception);

        // Schedule reconnection attempt
        final FeedType reconnectFeedType = feedType;
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                loggingService.logAction("Attempting to reconnect " + feedName + " feed after transport error");
                synchronized (AlpacaWebSocketClient.this) {
                    if (reconnectFeedType != null) {
                        reconnectFeed(reconnectFeedType);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        FeedType feedType = sessionFeedTypeMap.remove(session);
        String feedName = feedType != null ? (feedType == FeedType.STOCK ? "stock" : "crypto") : "unknown";

        loggingService.logAction("Alpaca " + feedName + " WebSocket connection closed: " + status);

        // Reset authentication state
        if (feedType == FeedType.STOCK) {
            stockAuthenticated = false;
            stockSession = null;
        } else if (feedType == FeedType.CRYPTO) {
            cryptoAuthenticated = false;
            cryptoSession = null;
        }

        // Attempt reconnect if we still have subscriptions for this feed
        final FeedType reconnectFeedType = feedType;
        if (reconnectFeedType != null && hasSubscriptionsForFeed(reconnectFeedType)) {
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    loggingService.logAction("Attempting to reconnect " + feedName + " feed with " +
                            getSubscriptionCountForFeed(reconnectFeedType) + " symbols");
                    synchronized (AlpacaWebSocketClient.this) {
                        reconnectFeed(reconnectFeedType);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Reconnects a specific feed if it's not already connected
     */
    private void reconnectFeed(FeedType feedType) {
        if (feedType == FeedType.STOCK) {
            if ((stockSession == null || !stockSession.isOpen()) && !stockSubscriptions.isEmpty()) {
                connectToFeed(FeedType.STOCK);
            }
        } else {
            if ((cryptoSession == null || !cryptoSession.isOpen()) && !cryptoSubscriptions.isEmpty()) {
                connectToFeed(FeedType.CRYPTO);
            }
        }
    }

    /**
     * Checks if there are subscriptions for a specific feed
     */
    private boolean hasSubscriptionsForFeed(FeedType feedType) {
        if (feedType == FeedType.STOCK) {
            return !stockSubscriptions.isEmpty();
        } else {
            return !cryptoSubscriptions.isEmpty();
        }
    }

    /**
     * Gets subscription count for a specific feed
     */
    private int getSubscriptionCountForFeed(FeedType feedType) {
        if (feedType == FeedType.STOCK) {
            return stockSubscriptions.size();
        } else {
            return cryptoSubscriptions.size();
        }
    }

    /**
     * Check if WebSocket is currently connected and authenticated
     * Returns true if at least one feed with subscriptions is connected
     */
    @Override
    public boolean isConnected() {
        boolean stockConnected = stockSubscriptions.isEmpty() ||
                (stockSession != null && stockSession.isOpen() && stockAuthenticated);
        boolean cryptoConnected = cryptoSubscriptions.isEmpty() ||
                (cryptoSession != null && cryptoSession.isOpen() && cryptoAuthenticated);

        return stockConnected && cryptoConnected;
    }

    /**
     * Get current subscription count (total across both feeds)
     */
    @Override
    public int getSubscriptionCount() {
        return stockSubscriptions.size() + cryptoSubscriptions.size();
    }

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}