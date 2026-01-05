package com.example.ehe_server.service.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockWebSocketSubscriptionManagerInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockCandleNotificationServiceInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockCandleProcessingServiceInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockCandleProcessingServiceInterface.CandleUpdateResult;
import com.example.ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int UPDATE_CHECK_INTERVAL_MS = 10_000;

    private final Map<String, StockCandleSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSubscriptionIds = new ConcurrentHashMap<>();

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final PlatformStockRepository platformStockRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final StockCandleNotificationServiceInterface notificationService;
    private final StockCandleProcessingServiceInterface processingService;
    private final LoggingServiceInterface loggingService;

    public StockWebSocketSubscriptionManager(
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository,
            PlatformStockRepository platformStockRepository,
            WebSocketSessionRegistry sessionRegistry,
            StockCandleNotificationServiceInterface notificationService,
            StockCandleProcessingServiceInterface processingService,
            LoggingServiceInterface loggingService) {
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.platformStockRepository = platformStockRepository;
        this.sessionRegistry = sessionRegistry;
        this.notificationService = notificationService;
        this.processingService = processingService;
        this.loggingService = loggingService;
    }

    // ==================== Subscription Management ====================

    @Override
    public StockCandleSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination) {

        validateSubscriptionRequest(userId, sessionId, platformName, stockSymbol, timeframe, destination);

        StockCandleSubscription subscription = createAndRegisterSubscription(
                userId, sessionId, platformName, stockSymbol, timeframe, destination);

        registerSessionCleanupIfNeeded(sessionId);

        sendInitialCandle(subscription);

        return new StockCandleSubscriptionResponse(subscription.getId());
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        StockCandleSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        removeFromSessionTracking(removed);

        loggingService.logAction("Cancelled candle subscription: " + subscriptionId);
    }

    // ==================== Scheduled Task ====================

    @Scheduled(fixedRate = UPDATE_CHECK_INTERVAL_MS)
    public void checkForUpdatesAndSendHeartbeats() {
        userContextService.setUser("SYSTEM", "SYSTEM");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        cleanupInvalidSubscriptions();

        activeSubscriptions.values().forEach(subscription ->
                processSubscriptionUpdate(subscription, now));

    }

    // ==================== Validation ====================

    private void validateSubscriptionRequest(
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination) {

        validateUserId(userId);
        validateSessionId(sessionId);
        validatePlatformName(platformName);
        validateStockSymbol(stockSymbol);
        validateTimeframe(timeframe);
        validateDestination(destination);
        validatePlatformStockCombination(platformName, stockSymbol);
    }

    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new MissingUserIdException();
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new MissingSessionIdException();
        }
    }

    private void validatePlatformName(String platformName) {
        if (platformName == null || platformName.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }
    }

    private void validateStockSymbol(String stockSymbol) {
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }
    }

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

    private void validateDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new MissingDestinationException();
        }
    }

    private void validatePlatformStockCombination(String platformName, String stockSymbol) {
        Optional<PlatformStock> platformStock = platformStockRepository
                .findByStockNameAndPlatformName(stockSymbol, platformName);
        if (platformStock.isEmpty()) {
            throw new PlatformStockNotFoundException(platformName, stockSymbol);
        }
    }

    // ==================== Subscription Helpers ====================

    private StockCandleSubscription createAndRegisterSubscription(
            Integer userId,
            String sessionId,
            String platformName,
            String stockSymbol,
            String timeframe,
            String destination) {

        String subscriptionId = UUID.randomUUID().toString();

        StockCandleSubscription subscription = new StockCandleSubscription(
                subscriptionId, userId, sessionId, platformName, stockSymbol, timeframe, destination);

        activeSubscriptions.put(subscriptionId, subscription);
        sessionToSubscriptionIds
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(subscriptionId);

        return subscription;
    }

    private void registerSessionCleanupIfNeeded(String sessionId) {
        Set<String> sessionSubs = sessionToSubscriptionIds.get(sessionId);
        if (sessionSubs != null && sessionSubs.size() == 1) {
            sessionRegistry.registerSessionCleanup(sessionId, () -> {
                cleanupSessionSubscriptions(sessionId);
            });
        }
    }

    private void removeFromSessionTracking(StockCandleSubscription subscription) {
        String sessionId = subscription.getSessionId();
        if (sessionId == null) {
            return;
        }

        Set<String> sessionSubs = sessionToSubscriptionIds.get(sessionId);
        if (sessionSubs != null) {
            sessionSubs.remove(subscription.getId());
            if (sessionSubs.isEmpty()) {
                sessionToSubscriptionIds.remove(sessionId);
            }
        }
    }

    private void cleanupSessionSubscriptions(String sessionId) {
        Set<String> subIds = sessionToSubscriptionIds.remove(sessionId);
        if (subIds != null && !subIds.isEmpty()) {
            subIds.forEach(activeSubscriptions::remove);
            loggingService.logAction("Auto-cleaned " + subIds.size() + " candle subscriptions for session " + sessionId);
        }
    }

    // ==================== Initial Candle ====================

    private void sendInitialCandle(StockCandleSubscription subscription) {
        try {
            Optional<CandleData> latestCandle = processingService.getLatestCandle(subscription);

            if (latestCandle.isPresent()) {
                CandleData candle = latestCandle.get();
                subscription.updateLatestCandle(candle);
                notificationService.sendInitialCandle(subscription, candle);
            } else {
                loggingService.logAction("No initial data available for subscription: " + subscription.getId());
            }
        } catch (Exception e) {
            loggingService.logError("Error sending initial data for subscription " +
                    subscription.getId() + ": " + e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Update Processing ====================

    private void processSubscriptionUpdate(StockCandleSubscription subscription, LocalDateTime now) {
        try {
            if (!hasValidRefreshToken(subscription)) {
                cancelSubscription(subscription.getId());
                return;
            }

            CandleUpdateResult result = processingService.checkForUpdates(subscription);

            if (result.hasUpdates()) {
                subscription.updateLatestCandle(result.latestCandle());
                notificationService.sendUpdate(subscription, result.candlesToSend(), now);
            } else {
                notificationService.sendHeartbeat(subscription, now);
            }

        } catch (Exception e) {
            loggingService.logError("Error checking updates for subscription " +
                    subscription.getId() + ": " + e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Subscription Cleanup ====================

    private void cleanupInvalidSubscriptions() {
        List<String> toRemove = activeSubscriptions.values().stream()
                .filter(sub -> !hasValidRefreshToken(sub))
                .map(StockCandleSubscription::getId)
                .toList();

        toRemove.forEach(this::cancelSubscription);
    }

    private boolean hasValidRefreshToken(StockCandleSubscription subscription) {
        List<JwtRefreshToken> tokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
        return tokens != null && !tokens.isEmpty();
    }
}