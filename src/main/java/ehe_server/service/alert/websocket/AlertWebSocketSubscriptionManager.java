package ehe_server.service.alert.websocket;

import ehe_server.dto.websocket.AlertSubscriptionResponse;
import ehe_server.entity.Alert;
import ehe_server.entity.JwtRefreshToken;
import ehe_server.entity.MarketCandle;
import ehe_server.exception.custom.InvalidSubscriptionIdException;
import ehe_server.exception.custom.MissingDestinationException;
import ehe_server.exception.custom.SubscriptionNotFoundException;
import ehe_server.repository.AlertRepository;
import ehe_server.repository.JwtRefreshTokenRepository;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.alert.websocket.AlertNotificationServiceInterface;
import ehe_server.service.intf.alert.websocket.AlertProcessingServiceInterface;
import ehe_server.service.intf.alert.websocket.AlertWebSocketSubscriptionManagerInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AlertWebSocketSubscriptionManager implements AlertWebSocketSubscriptionManagerInterface {

    private static final int ALERT_CHECK_INTERVAL_MS = 60_000;
    private static final int UTC_OFFSET_HOURS = 2;

    private final Map<String, AlertSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSubscriptionIds = new ConcurrentHashMap<>();

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final AlertProcessingServiceInterface processingService;
    private final AlertNotificationServiceInterface notificationService;
    private final LoggingServiceInterface loggingService;
    private final AlertRepository alertRepository;

    public AlertWebSocketSubscriptionManager(
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository,
            WebSocketSessionRegistry sessionRegistry,
            AlertProcessingServiceInterface processingService,
            AlertNotificationServiceInterface notificationService,
            LoggingServiceInterface loggingService,
            AlertRepository alertRepository) {
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.sessionRegistry = sessionRegistry;
        this.processingService = processingService;
        this.notificationService = notificationService;
        this.loggingService = loggingService;
        this.alertRepository = alertRepository;
    }

    @Override
    public AlertSubscriptionResponse createSubscription(Integer userId, String sessionId, String destination) {
        validateDestination(destination);

        AlertSubscription subscription = createAndRegisterSubscription(userId, sessionId, destination);
        registerSessionCleanupIfNeeded(sessionId);

        loggingService.logAction(String.format("Created alert subscription: %s for user: %d, session: %s",
                subscription.getId(), userId, sessionId));

        performInitialAlertCheckAsync(subscription);

        return new AlertSubscriptionResponse(subscription.getId());
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        AlertSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        removeFromSessionTracking(removed);
        loggingService.logAction("Cancelled alert subscription: " + subscriptionId);
    }

    @Scheduled(fixedRate = ALERT_CHECK_INTERVAL_MS)
    public void checkForNewAlerts() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        cleanupInvalidSubscriptions();

        activeSubscriptions.values().stream()
                .filter(AlertSubscription::isInitialCheckCompleted)
                .filter(subscription -> shouldCheckSubscription(subscription, currentMinute))
                .forEach(subscription -> processSubscriptionAlerts(subscription, currentMinute));
    }

    // ==================== Subscription Management ====================

    private void validateDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new MissingDestinationException();
        }
    }

    private AlertSubscription createAndRegisterSubscription(Integer userId, String sessionId, String destination) {
        String subscriptionId = UUID.randomUUID().toString();
        AlertSubscription subscription = new AlertSubscription(subscriptionId, userId, sessionId, destination);

        activeSubscriptions.put(subscriptionId, subscription);
        sessionToSubscriptionIds
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(subscriptionId);

        return subscription;
    }

    private void registerSessionCleanupIfNeeded(String sessionId) {
        Set<String> sessionSubs = sessionToSubscriptionIds.get(sessionId);
        if (sessionSubs != null && sessionSubs.size() == 1) {
            sessionRegistry.registerSessionCleanup(sessionId, () -> cleanupSessionSubscriptions(sessionId));
        }
    }

    private void removeFromSessionTracking(AlertSubscription subscription) {
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
            loggingService.logAction("Auto-cleaned " + subIds.size() + " subscriptions for session " + sessionId);
        }
    }

    // ==================== Initial Alert Check ====================

    @Async
    protected void performInitialAlertCheckAsync(AlertSubscription subscription) {
        try {
            performInitialAlertCheck(subscription);
        } catch (Exception e) {
            loggingService.logAction("Error during initial alert check for subscription " +
                    subscription.getId() + ": " + e.getMessage());
        }
    }

    private void performInitialAlertCheck(AlertSubscription subscription) {
        List<Alert> userAlerts = alertRepository.findByUser_UserId(subscription.getUserId());

        if (userAlerts.isEmpty()) {
            loggingService.logAction("No active alerts found for initial check");
            subscription.markInitialCheckCompleted();
            return;
        }

        loggingService.logAction("Starting initial check for " + userAlerts.size() + " alerts");

        for (Alert alert : userAlerts) {
            processInitialAlertCheck(alert, subscription);
        }

        subscription.markInitialCheckCompleted();
        subscription.updateLastCheckedMinuteCandle(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        loggingService.logAction("Completed initial alert check for subscription: " + subscription.getId());
    }

    private void processInitialAlertCheck(Alert alert, AlertSubscription subscription) {
        LocalDateTime checkStartTime = alert.getDateCreated()
                .truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(1)
                .minusHours(UTC_OFFSET_HOURS);

        loggingService.logAction(String.format("Checking alert #%d for %s:%s starting from %s",
                alert.getAlertId(),
                alert.getPlatformStock().getPlatform().getPlatformName(),
                alert.getPlatformStock().getStock().getStockSymbol(),
                checkStartTime));

        Optional<MarketCandle> triggeringCandle = processingService.checkAlertAgainstTimeframe(
                alert,
                MarketCandle.Timeframe.M1,
                checkStartTime,
                LocalDateTime.now());

        triggeringCandle.ifPresent(marketCandle -> handleTriggeredAlert(alert, marketCandle, subscription));
    }

    // ==================== Scheduled Alert Checking ====================

    private boolean shouldCheckSubscription(AlertSubscription subscription, LocalDateTime currentMinute) {
        LocalDateTime lastChecked = subscription.getLastCheckedMinuteCandle();
        return lastChecked == null || currentMinute.isAfter(lastChecked);
    }

    private void processSubscriptionAlerts(AlertSubscription subscription, LocalDateTime currentMinute) {
        List<Alert> activeAlerts = alertRepository.findByUser_UserId(subscription.getUserId());

        if (activeAlerts.isEmpty()) {
            subscription.updateLastCheckedMinuteCandle(currentMinute);
            return;
        }

        loggingService.logAction("Checking " + activeAlerts.size() + " active alerts for latest minute");

        for (Alert alert : activeAlerts) {
            checkLatestCandleForAlert(alert, subscription);
        }

        subscription.updateLastCheckedMinuteCandle(currentMinute);
    }

    private void checkLatestCandleForAlert(Alert alert, AlertSubscription subscription) {
        Optional<MarketCandle> latestCandle = processingService.getLatestMinuteCandle(alert);

        if (latestCandle.isEmpty()) {
            return;
        }

        MarketCandle candle = latestCandle.get();

        boolean isNewCandle = subscription.getLastCheckedMinuteCandle() == null ||
                candle.getTimestamp().isAfter(subscription.getLastCheckedMinuteCandle().minusHours(UTC_OFFSET_HOURS));

        if (!isNewCandle) {
            return;
        }

        loggingService.logAction("Found new candle at " + candle.getTimestamp() +
                " for alert #" + alert.getAlertId());

        Optional<MarketCandle> triggeringCandle = processingService.checkAlertAgainstCandle(alert, candle);

        triggeringCandle.ifPresent(marketCandle -> handleTriggeredAlert(alert, marketCandle, subscription));
    }

    // ==================== Alert Trigger Handling ====================

    private void handleTriggeredAlert(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription) {
        notificationService.sendTriggeredNotification(alert, triggeringCandle, subscription);
        processingService.deleteAlert(alert);
        loggingService.logAction("Alert #" + alert.getAlertId() + " triggered and deactivated");
    }

    // ==================== Subscription Cleanup ====================

    private void cleanupInvalidSubscriptions() {
        List<String> toRemove = activeSubscriptions.values().stream()
                .filter(this::hasNoValidRefreshToken)
                .map(AlertSubscription::getId)
                .toList();

        toRemove.forEach(this::cancelSubscription);
    }

    private boolean hasNoValidRefreshToken(AlertSubscription subscription) {
        List<JwtRefreshToken> tokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
        return tokens == null || tokens.isEmpty();
    }
}