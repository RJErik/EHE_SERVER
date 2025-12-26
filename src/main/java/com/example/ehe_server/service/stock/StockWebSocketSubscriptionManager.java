package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.CandleUpdateMessage;
import com.example.ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.MarketCandleServiceInterface;
import com.example.ehe_server.service.intf.stock.StockWebSocketSubscriptionManagerInterface;
import com.example.ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class StockWebSocketSubscriptionManager implements StockWebSocketSubscriptionManagerInterface {

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final PlatformStockRepository platformStockRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final MarketCandleServiceInterface marketCandleService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;

    private static class Subscription {
        private final String id;
        private final Integer userId;
        private final String sessionId;
        private final String platformName;
        private final String stockSymbol;
        private final String timeframe;
        private final String destination;

        // Track the latest candle for modification detection
        private LocalDateTime latestCandleTimestamp;
        private BigDecimal latestCandleOpen;
        private BigDecimal latestCandleHigh;
        private BigDecimal latestCandleLow;
        private BigDecimal latestCandleClose;
        private BigDecimal latestCandleVolume;

        public Subscription(
                String id,
                Integer userId,
                String sessionId,
                String platformName,
                String stockSymbol,
                String timeframe,
                String destination) {
            this.id = id;
            this.userId = userId;
            this.sessionId = sessionId;
            this.platformName = platformName;
            this.stockSymbol = stockSymbol;
            this.timeframe = timeframe;
            this.destination = destination;
        }

        // Getters
        public String getId() {
            return id;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getPlatformName() {
            return platformName;
        }

        public String getStockSymbol() {
            return stockSymbol;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public String getDestination() {
            return destination;
        }

        public LocalDateTime getLatestCandleTimestamp() {
            return latestCandleTimestamp;
        }

        public BigDecimal getLatestCandleOpen() {
            return latestCandleOpen;
        }

        public BigDecimal getLatestCandleHigh() {
            return latestCandleHigh;
        }

        public BigDecimal getLatestCandleLow() {
            return latestCandleLow;
        }

        public BigDecimal getLatestCandleClose() {
            return latestCandleClose;
        }

        public BigDecimal getLatestCandleVolume() {
            return latestCandleVolume;
        }

        public void updateLatestCandle(CandleData candle) {
            if (candle != null) {
                this.latestCandleTimestamp = candle.getTimestamp();
                this.latestCandleOpen = candle.getOpenPrice();
                this.latestCandleHigh = candle.getHighPrice();
                this.latestCandleLow = candle.getLowPrice();
                this.latestCandleClose = candle.getClosePrice();
                this.latestCandleVolume = candle.getVolume();
            }
        }
    }

    private final Map<String, Subscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSubscriptionIds = new ConcurrentHashMap<>();

    public StockWebSocketSubscriptionManager(
            MarketCandleServiceInterface marketCandleService,
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository,
            PlatformStockRepository platformStockRepository,
            WebSocketSessionRegistry sessionRegistry) {
        this.marketCandleService = marketCandleService;
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.platformStockRepository = platformStockRepository;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Create a new subscription and send the latest candle immediately.
     * Validates all parameters individually before creating the subscription.
     */
    public StockCandleSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination) {

        // Validate each parameter individually
        validateUserId(userId);
        validateSessionId(sessionId);
        validatePlatformName(platformName);
        validateStockSymbol(stockSymbol);
        validateTimeframe(timeframe);
        validateDestination(destination);

        // Validate that the platform/stock combination exists
        validatePlatformStockCombination(platformName, stockSymbol);

        String subscriptionId = UUID.randomUUID().toString();

        Subscription subscription = new Subscription(
                subscriptionId,
                userId,
                sessionId,
                platformName,
                stockSymbol,
                timeframe,
                destination);

        activeSubscriptions.put(subscriptionId, subscription);

        // Track by session
        boolean isFirstForSession = sessionToSubscriptionIds
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .isEmpty();
        sessionToSubscriptionIds.get(sessionId).add(subscriptionId);

        // Register cleanup callback with registry (only once per session)
        if (isFirstForSession) {
            sessionRegistry.registerSessionCleanup(sessionId, () -> {
                System.out.println("[CandleManager] Cleaning up candle subscriptions for session: " + sessionId);
                cleanupSessionSubscriptions(sessionId);
            });
        }

        System.out.println("Created subscription: " + subscriptionId + " for session: " + sessionId);
        System.out.println("Total active subscriptions: " + activeSubscriptions.size());

        // ✅ Send the latest candle immediately
        try {
            CandleData latestCandle = marketCandleService.getLatestCandle(
                    platformName,
                    stockSymbol,
                    timeframe);

            if (latestCandle != null) {
                // Initialize tracking with the latest candle
                subscription.updateLatestCandle(latestCandle);

                // Send the initial candle
                CandleUpdateMessage initialMessage = new CandleUpdateMessage();
                initialMessage.setSubscriptionId(subscriptionId);
                initialMessage.setUpdateType("INITIAL");
                initialMessage.setUpdatedCandles(List.of(latestCandle));
                initialMessage.setUpdateTimestamp(LocalDateTime.now(ZoneOffset.UTC));

                messagingTemplate.convertAndSend(destination, initialMessage);

                System.out.println("Sent initial candle: " + latestCandle.getTimestamp() +
                        " | Seq:" + latestCandle.getSequence() +
                        " | O:" + latestCandle.getOpenPrice() +
                        " H:" + latestCandle.getHighPrice() +
                        " L:" + latestCandle.getLowPrice() +
                        " C:" + latestCandle.getClosePrice() +
                        " V:" + latestCandle.getVolume());

                loggingService.logAction("Sent initial candle for subscription: " + subscriptionId +
                        " (" + platformName + ":" + stockSymbol + " " + timeframe + ")");
            } else {
                System.out.println("No initial candle available for subscription: " + subscriptionId);
                loggingService.logAction("No initial data available for subscription: " + subscriptionId);
            }
        } catch (Exception e) {
            System.out.println("ERROR sending initial candle: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error sending initial data for subscription " +
                    subscriptionId + ": " + e.getMessage(), e);
        }

        return new StockCandleSubscriptionResponse(subscriptionId);
    }

    /**
     * Validates that userId is not null.
     * Throws MissingUserIdException if validation fails.
     */
    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new MissingUserIdException();
        }
    }

    /**
     * Validates that sessionId is not null or empty.
     * Throws MissingSessionIdException if validation fails.
     */
    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new MissingSessionIdException();
        }
    }

    /**
     * Validates that platformName is not null or empty.
     * Throws MissingPlatformNameException if validation fails.
     */
    private void validatePlatformName(String platformName) {
        if (platformName == null || platformName.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }
    }

    /**
     * Validates that stockSymbol is not null or empty.
     * Throws MissingStockSymbolException if validation fails.
     */
    private void validateStockSymbol(String stockSymbol) {
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }
    }

    /**
     * Validates that timeframe is not null or empty, and can be converted to a valid Timeframe enum.
     * Throws MissingTimeframeException if timeframe is missing.
     * Throws InvalidTimeframeException if timeframe cannot be converted to a valid enum value.
     */
    private void validateTimeframe(String timeframe) {
        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new MissingTimeframeException();
        }

        try {
            MarketCandle.Timeframe.fromValue(timeframe);
        } catch (IllegalArgumentException e) {
            throw new InvalidTimeframeException(timeframe);
        }
    }

    /**
     * Validates that destination is not null or empty.
     * Throws MissingDestinationException if validation fails.
     */
    private void validateDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new MissingDestinationException();
        }
    }

    /**
     * Validates that the platform/stock combination exists in the database.
     * Throws PlatformStockNotFoundException if the combination does not exist.
     */
    private void validatePlatformStockCombination(String platformName, String stockSymbol) {
        Optional<PlatformStock> platformStock = platformStockRepository
                .findByStockNameAndPlatformName(stockSymbol, platformName);

        if (platformStock.isEmpty()) {
            throw new PlatformStockNotFoundException(platformName, stockSymbol);
        }
    }

    /**
     * Cancel a subscription (explicit unsubscribe)
     */
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }
        Subscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        // Remove from session tracking
        String sessionId = removed.getSessionId();
        if (sessionId != null) {
            Set<String> sessionSubs = sessionToSubscriptionIds.get(sessionId);
            if (sessionSubs != null) {
                sessionSubs.remove(subscriptionId);
                if (sessionSubs.isEmpty()) {
                    sessionToSubscriptionIds.remove(sessionId);
                }
            }
        }

        System.out.println("Cancelled subscription: " + subscriptionId);
        System.out.println("Remaining active subscriptions: " + activeSubscriptions.size());

        loggingService.logAction("Cancelled candle subscription: " + subscriptionId);
    }

    /**
     * Called automatically by WebSocketSessionRegistry when session disconnects
     */
    private void cleanupSessionSubscriptions(String sessionId) {
        Set<String> subIds = sessionToSubscriptionIds.remove(sessionId);

        if (subIds != null && !subIds.isEmpty()) {
            subIds.forEach(subId -> {
                Subscription removed = activeSubscriptions.remove(subId);
                if (removed != null) {
                    System.out.println("[CandleManager] Auto-removed subscription: " + subId);
                }
            });

            System.out.println("[CandleManager] Session cleanup complete. Removed " + subIds.size() + " subscriptions");
            loggingService.logAction("Auto-cleaned " + subIds.size() + " candle subscriptions for session " + sessionId);
        }
    }

    /**
     * Scheduled task to check for updates and send them to clients
     * Runs every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void checkForUpdatesAndSendHeartbeats() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        System.out.println("\n=== Checking for candle updates (Active subscriptions: " +
                activeSubscriptions.size() + ") ===");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        activeSubscriptions.values().forEach(subscription -> {
            try {
                // Check if user has valid refresh tokens
                List<JwtRefreshToken> userTokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
                if (userTokens == null || userTokens.isEmpty()) {
                    System.out.println("User " + subscription.getUserId() +
                            " has no refresh tokens. Disconnecting subscription " + subscription.getId());
                    cancelSubscription(subscription.getId());
                    return;
                }

                List<CandleData> candlesToSend = new java.util.ArrayList<>();
                boolean hasUpdates = false;

                // Get the current latest candle from the database
                CandleData currentLatestCandle = marketCandleService.getLatestCandle(
                        subscription.getPlatformName(),
                        subscription.getStockSymbol(),
                        subscription.getTimeframe());

                if (currentLatestCandle == null) {
                    System.out.println("No latest candle found for subscription: " + subscription.getId());
                    sendHeartbeat(subscription, now);
                    return;
                }

                // Check if we have a new candle (different timestamp)
                if (!currentLatestCandle.getTimestamp().equals(subscription.getLatestCandleTimestamp())) {
                    System.out.println("New candle detected at: " + currentLatestCandle.getTimestamp());

                    // Before sending the new candle, check if the previous one was modified
                    if (subscription.getLatestCandleTimestamp() != null) {
                        CandleData modifiedPreviousCandle = marketCandleService.getModifiedCandle(
                                subscription.getPlatformName(),
                                subscription.getStockSymbol(),
                                subscription.getTimeframe(),
                                subscription.getLatestCandleTimestamp(),
                                subscription.getLatestCandleOpen(),
                                subscription.getLatestCandleHigh(),
                                subscription.getLatestCandleLow(),
                                subscription.getLatestCandleClose(),
                                subscription.getLatestCandleVolume());

                        if (modifiedPreviousCandle != null) {
                            System.out.println("Previous candle was modified, adding to updates");
                            candlesToSend.add(modifiedPreviousCandle);
                        }
                    }

                    // Add the new latest candle
                    candlesToSend.add(currentLatestCandle);
                    hasUpdates = true;

                    // Update subscription tracking
                    subscription.updateLatestCandle(currentLatestCandle);

                } else {
                    // Same timestamp, check if the current candle was modified
                    CandleData modifiedCandle = marketCandleService.getModifiedCandle(
                            subscription.getPlatformName(),
                            subscription.getStockSymbol(),
                            subscription.getTimeframe(),
                            subscription.getLatestCandleTimestamp(),
                            subscription.getLatestCandleOpen(),
                            subscription.getLatestCandleHigh(),
                            subscription.getLatestCandleLow(),
                            subscription.getLatestCandleClose(),
                            subscription.getLatestCandleVolume());

                    if (modifiedCandle != null) {
                        System.out.println("Current candle was modified");
                        candlesToSend.add(modifiedCandle);
                        hasUpdates = true;

                        // Update subscription tracking
                        subscription.updateLatestCandle(modifiedCandle);
                    }
                }

                // Send updates or heartbeat
                if (hasUpdates) {
                    sendUpdate(subscription, candlesToSend, now);
                } else {
                    sendHeartbeat(subscription, now);
                }

            } catch (Exception e) {
                System.out.println("ERROR checking updates for subscription " +
                        subscription.getId() + ": " + e.getMessage());
                e.printStackTrace();
                loggingService.logError("Error checking updates for subscription " +
                        subscription.getId() + ": " + e.getMessage(), e);
            }
        });

        System.out.println("=== Update check completed ===\n");
    }

    private void sendUpdate(Subscription subscription, List<CandleData> candles, LocalDateTime now) {
        CandleUpdateMessage updateMessage = new CandleUpdateMessage();
        updateMessage.setSubscriptionId(subscription.getId());
        updateMessage.setUpdateType("UPDATE");
        updateMessage.setUpdatedCandles(candles);
        updateMessage.setUpdateTimestamp(now);

        messagingTemplate.convertAndSend(subscription.getDestination(), updateMessage);

        System.out.println(">>> SENT " + candles.size() + " candle update(s) for subscription " +
                subscription.getId() + " <<<");
        candles.forEach(candle -> {
            System.out.println("  Candle: " + candle.getTimestamp() +
                    " | Seq:" + candle.getSequence() +
                    " | O:" + candle.getOpenPrice() +
                    " H:" + candle.getHighPrice() +
                    " L:" + candle.getLowPrice() +
                    " C:" + candle.getClosePrice() +
                    " V:" + candle.getVolume());
        });

        loggingService.logAction("Sent " + candles.size() + " candle update(s) for subscription " +
                subscription.getId());
    }

    private void sendHeartbeat(Subscription subscription, LocalDateTime now) {
        CandleUpdateMessage heartbeatMessage = new CandleUpdateMessage();
        heartbeatMessage.setSubscriptionId(subscription.getId());
        heartbeatMessage.setUpdateType("HEARTBEAT");
        heartbeatMessage.setUpdatedCandles(new java.util.ArrayList<>());
        heartbeatMessage.setUpdateTimestamp(now);

        messagingTemplate.convertAndSend(subscription.getDestination(), heartbeatMessage);

        System.out.println("Sent heartbeat for subscription: " + subscription.getId());
    }
}