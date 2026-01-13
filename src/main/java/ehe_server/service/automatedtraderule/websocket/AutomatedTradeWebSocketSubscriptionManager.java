package ehe_server.service.automatedtraderule.websocket;

import ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.JwtRefreshToken;
import ehe_server.entity.MarketCandle;
import ehe_server.exception.custom.*;
import ehe_server.repository.AutomatedTradeRuleRepository;
import ehe_server.repository.JwtRefreshTokenRepository;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeNotificationServiceInterface;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeProcessingServiceInterface;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeProcessingServiceInterface.TradeExecutionResult;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeWebSocketSubscriptionManagerInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutomatedTradeWebSocketSubscriptionManager implements AutomatedTradeWebSocketSubscriptionManagerInterface {

    private static final int RULE_CHECK_INTERVAL_MS = 60_000;

    private final Map<String, AutomatedTradeSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSubscriptionIds = new ConcurrentHashMap<>();

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final AutomatedTradeProcessingServiceInterface processingService;
    private final AutomatedTradeNotificationServiceInterface notificationService;
    private final LoggingServiceInterface loggingService;
    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;

    public AutomatedTradeWebSocketSubscriptionManager(
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository,
            WebSocketSessionRegistry sessionRegistry,
            AutomatedTradeProcessingServiceInterface processingService,
            AutomatedTradeNotificationServiceInterface notificationService,
            LoggingServiceInterface loggingService,
            AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.sessionRegistry = sessionRegistry;
        this.processingService = processingService;
        this.notificationService = notificationService;
        this.loggingService = loggingService;
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @Override
    public AutomatedTradeSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String destination) {

        validateSubscriptionRequest(sessionId, destination);

        AutomatedTradeSubscription subscription = createAndRegisterSubscription(userId, sessionId, destination);
        registerSessionCleanupIfNeeded(sessionId);

        loggingService.logAction(String.format(
                "Created automated trade subscription: %s for user: %d, session: %s",
                subscription.getId(), userId, sessionId));

        return new AutomatedTradeSubscriptionResponse(subscription.getId());
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        AutomatedTradeSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        removeFromSessionTracking(removed);
        loggingService.logAction("Cancelled automated trade subscription: " + subscriptionId);
    }

    @Override
    public Collection<AutomatedTradeSubscription> getSubscriptionsForUser(Integer userId) {
        return activeSubscriptions.values().stream()
                .filter(sub -> sub.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = RULE_CHECK_INTERVAL_MS)
    public void checkAutomatedTradeRules() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        loggingService.logAction("Checking automated trade rules at " + currentMinute);

        cleanupInvalidSubscriptions();
        processAllActiveRules();
    }

    private void validateSubscriptionRequest(String sessionId, String destination) {
        validateSessionId(sessionId);
        validateDestination(destination);
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new MissingSessionIdException();
        }
    }

    private void validateDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new MissingDestinationException();
        }
    }

    private AutomatedTradeSubscription createAndRegisterSubscription(
            Integer userId, String sessionId, String destination) {

        String subscriptionId = UUID.randomUUID().toString();
        AutomatedTradeSubscription subscription = new AutomatedTradeSubscription(
                subscriptionId, userId, sessionId, destination);

        activeSubscriptions.put(subscriptionId, subscription);
        sessionToSubscriptionIds
                .computeIfAbsent(sessionId, _ -> ConcurrentHashMap.newKeySet())
                .add(subscriptionId);

        return subscription;
    }

    private void registerSessionCleanupIfNeeded(String sessionId) {
        Set<String> sessionSubs = sessionToSubscriptionIds.get(sessionId);
        if (sessionSubs != null && sessionSubs.size() == 1) {
            sessionRegistry.registerSessionCleanup(sessionId, () -> cleanupSessionSubscriptions(sessionId));
        }
    }

    private void removeFromSessionTracking(AutomatedTradeSubscription subscription) {
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
            loggingService.logAction("Auto-cleaned " + subIds.size() +
                    " trade subscriptions for session " + sessionId);
        }
    }

    private void processAllActiveRules() {
        try {
            List<AutomatedTradeRule> activeRules = automatedTradeRuleRepository.findAll();

            if (activeRules.isEmpty()) {
                return;
            }

            for (AutomatedTradeRule rule : activeRules) {
                processRule(rule);
            }
        } catch (Exception e) {
            loggingService.logError("Error in automated trade rule check: " + e.getMessage(), e);
        }
    }

    private void processRule(AutomatedTradeRule rule) {
        try {
            Optional<MarketCandle> latestCandleOpt = processingService.getLatestMinuteCandle(rule.getPlatformStock());

            if (latestCandleOpt.isEmpty()) {
                loggingService.logAction("No candle data found for " +
                        rule.getPlatformStock().getStock().getStockSymbol() +
                        " when checking rule #" + rule.getAutomatedTradeRuleId());
                return;
            }

            MarketCandle latestCandle = latestCandleOpt.get();

            if (processingService.checkRuleCondition(rule, latestCandle)) {
                handleTriggeredRule(rule, latestCandle);
            }

        } catch (Exception e) {
            loggingService.logError("Error processing automated trade rule #" +
                    rule.getAutomatedTradeRuleId() + ": " + e.getMessage(), e);
        }
    }

    private void handleTriggeredRule(AutomatedTradeRule rule, MarketCandle triggeringCandle) {
        Integer userId = rule.getUser().getUserId();
        Collection<AutomatedTradeSubscription> userSubscriptions = getSubscriptionsForUser(userId);

        TradeExecutionResult result = processingService.executeAutomatedTrade(rule, triggeringCandle);

        // Send notification to all user's subscriptions
        for (AutomatedTradeSubscription subscription : userSubscriptions) {
            notificationService.sendTradeNotification(
                    rule,
                    triggeringCandle,
                    result.success(),
                    result.tradeResponse(),
                    result.transactionId(),
                    subscription);
        }

        // Delete the rule after execution (regardless of success)
        automatedTradeRuleRepository.delete(rule);
        loggingService.logAction("Automated trade rule #" + rule.getAutomatedTradeRuleId() +
                " executed and deactivated. Trade success: " + result.success());
    }

    private void cleanupInvalidSubscriptions() {
        List<String> toRemove = activeSubscriptions.values().stream()
                .filter(this::hasNoValidRefreshToken)
                .map(AutomatedTradeSubscription::getId)
                .toList();

        for (String subscriptionId : toRemove) {
            try {
                cancelSubscription(subscriptionId);
            } catch (Exception e) {
                loggingService.logError("Error removing subscription " + subscriptionId + ": " + e.getMessage(), e);
            }
        }
    }

    private boolean hasNoValidRefreshToken(AutomatedTradeSubscription subscription) {
        List<JwtRefreshToken> tokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
        return tokens == null || tokens.isEmpty();
    }
}