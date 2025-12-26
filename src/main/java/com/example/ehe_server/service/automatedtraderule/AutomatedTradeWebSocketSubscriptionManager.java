package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.dto.websocket.AutomatedTradeNotificationResponse;
import com.example.ehe_server.dto.websocket.AutomatedTradeSubscriptionResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeWebSocketSubscriptionManagerInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import com.example.ehe_server.service.websocket.WebSocketSessionRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutomatedTradeWebSocketSubscriptionManager implements AutomatedTradeWebSocketSubscriptionManagerInterface {

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final LoggingServiceInterface loggingService;
    private final TradingServiceInterface tradingService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static class AutomatedTradeSubscription {
        private final String id;
        private final Integer userId;
        private final String sessionId;
        private final String destination;

        public AutomatedTradeSubscription(String id, Integer userId, String sessionId, String destination) {
            this.id = id;
            this.userId = userId;
            this.sessionId = sessionId;
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

        public String getDestination() {
            return destination;
        }
    }

    private final Map<String, AutomatedTradeSubscription> activeSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSubscriptionIds = new ConcurrentHashMap<>();

    public AutomatedTradeWebSocketSubscriptionManager(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            MarketCandleRepository marketCandleRepository,
            SimpMessagingTemplate simpMessagingTemplate,
            LoggingServiceInterface loggingService,
            TradingServiceInterface tradingService,
            TransactionRepository transactionRepository,
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository,
            WebSocketSessionRegistry sessionRegistry,
            UserRepository userRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.loggingService = loggingService;
        this.tradingService = tradingService;
        this.transactionRepository = transactionRepository;
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
        this.sessionRegistry = sessionRegistry;
        this.userRepository = userRepository;
    }

    /**
     * Create a new subscription for automated trades and return its ID.
     * Validates all parameters individually before creating the subscription.
     */
    public AutomatedTradeSubscriptionResponse createSubscription(
            Integer userId,
            String sessionId,
            String destination) {

        // Validate each parameter individually
        validateUserId(userId);
        validateSessionId(sessionId);
        validateDestination(destination);
        validateUserExists(userId);

        String subscriptionId = UUID.randomUUID().toString();

        AutomatedTradeSubscription subscription = new AutomatedTradeSubscription(
                subscriptionId,
                userId,
                sessionId,
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
                System.out.println("[TradeManager] Cleaning up trade subscriptions for session: " + sessionId);
                cleanupSessionSubscriptions(sessionId);
            });
        }

        System.out.println("Created subscription: " + subscriptionId + " for session: " + sessionId);
        System.out.println("Total active subscriptions: " + activeSubscriptions.size());

        loggingService.logAction("Created automated trade subscription: " + subscriptionId +
                " for user: " + userId + ", session: " + sessionId);

        return new AutomatedTradeSubscriptionResponse(subscriptionId);
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
     * Validates that destination is not null or empty.
     * Throws MissingDestinationException if validation fails.
     */
    private void validateDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new MissingDestinationException();
        }
    }

    /**
     * Validates that the user exists in the database.
     * Throws UserNotFoundException if the user does not exist.
     */
    private void validateUserExists(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    /**
     * Cancel a subscription (explicit unsubscribe)
     */
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        AutomatedTradeSubscription removed = activeSubscriptions.remove(subscriptionId);

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

        loggingService.logAction("Cancelled automated trade subscription: " + subscriptionId);
    }

    /**
     * Called automatically by WebSocketSessionRegistry when session disconnects
     */
    private void cleanupSessionSubscriptions(String sessionId) {
        Set<String> subIds = sessionToSubscriptionIds.remove(sessionId);

        if (subIds != null && !subIds.isEmpty()) {
            subIds.forEach(subId -> {
                AutomatedTradeSubscription removed = activeSubscriptions.remove(subId);
                if (removed != null) {
                    System.out.println("[TradeManager] Auto-removed subscription: " + subId);
                }
            });

            System.out.println("[TradeManager] Session cleanup complete. Removed " + subIds.size() + " subscriptions");
            loggingService.logAction("Auto-cleaned " + subIds.size() + " trade subscriptions for session " + sessionId);
        }
    }

    /**
     * Scheduled task to check for automated trade rules every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkAutomatedTradeRules() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);

        loggingService.logAction("Checking automated trade rules at " + currentMinute);

        // Validate active subscriptions and remove those without refresh tokens
        validateAndCleanupSubscriptions();

        try {
            // Get all active automated trade rules
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

    /**
     * Validate active subscriptions and remove those without valid refresh tokens
     */
    private void validateAndCleanupSubscriptions() {
        List<String> subscriptionsToRemove = new ArrayList<>();

        for (AutomatedTradeSubscription subscription : activeSubscriptions.values()) {
            List<JwtRefreshToken> userTokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
            if (userTokens == null || userTokens.isEmpty()) {
                System.out.println("User " + subscription.getUserId() +
                        " has no refresh tokens. Disconnecting subscription " + subscription.getId());
                subscriptionsToRemove.add(subscription.getId());
            }
        }

        // Remove invalid subscriptions
        for (String subscriptionId : subscriptionsToRemove) {
            try {
                cancelSubscription(subscriptionId);
            } catch (Exception e) {
                loggingService.logError("Error removing subscription " + subscriptionId + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Process a single automated trade rule
     */
    private void processRule(AutomatedTradeRule rule) {
        try {
            PlatformStock platformStock = rule.getPlatformStock();

            // Look for the latest 1-minute candle
            MarketCandle latestCandle = marketCandleRepository
                    .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            platformStock, MarketCandle.Timeframe.M1);

            if (latestCandle == null) {
                loggingService.logAction("No candle data found for " + platformStock.getStock().getStockName() +
                        " when checking rule #" + rule.getAutomatedTradeRuleId());
                return;
            }

            // Check if rule condition is triggered
            boolean isTriggered = checkRuleCondition(rule, latestCandle);

            if (isTriggered) {
                // Execute the trade
                executeAutomatedTrade(rule, latestCandle);
            }
        } catch (Exception e) {
            loggingService.logError("Error processing automated trade rule #" + rule.getAutomatedTradeRuleId() +
                    ": " + e.getMessage(), e);
        }
    }

    /**
     * Check if a rule condition is triggered by current market data
     */
    private boolean checkRuleCondition(AutomatedTradeRule rule, MarketCandle candle) {
        BigDecimal priceToCheck;
        BigDecimal thresholdValue = rule.getThresholdValue();

        // For price above alerts, check the close price
        // For price below alerts, check the close price
        if (rule.getConditionType() == AutomatedTradeRule.ConditionType.PRICE_ABOVE) {
            priceToCheck = candle.getClosePrice();
            return priceToCheck.compareTo(thresholdValue) > 0;
        } else { // PRICE_BELOW
            priceToCheck = candle.getClosePrice();
            return priceToCheck.compareTo(thresholdValue) < 0;
        }
    }

    /**
     * Execute the automated trade and send notification
     */
    private void executeAutomatedTrade(AutomatedTradeRule rule, MarketCandle triggeringCandle) {
        int userId = rule.getUser().getUserId();

        try {
            loggingService.logAction("Executing automated trade for rule #" + rule.getAutomatedTradeRuleId());

            // Determine action parameters
            Integer portfolioId = rule.getPortfolio().getPortfolioId();
            String stockSymbol = rule.getPlatformStock().getStock().getStockName();
            String action = rule.getActionType().toString();
            BigDecimal quantity = rule.getQuantity();
            String quantityType = rule.getQuantityType().toString();

            // Execute the market order
            TradeExecutionResponse tradeResult = tradingService.executeTrade(
                    userId, portfolioId, stockSymbol, action, quantity, quantityType);

            boolean success = tradeResult != null && "FILLED".equals(tradeResult.getStatus());

            // Extract transaction ID if available
            Integer transactionId = null;
            if (success && tradeResult.getOrderId() != null) {
                // Find our transaction based on the order ID or other criteria
                // For this example, we'll use the latest transaction for this portfolio
                List<Transaction> transactions = transactionRepository.findByPortfolio_PortfolioId(portfolioId);
                if (!transactions.isEmpty()) {
                    // Sort by transaction date descending to get the most recent
                    transactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));
                    transactionId = transactions.getFirst().getTransactionId();
                }
            }

            // Delete the rule after execution regardless of success
            automatedTradeRuleRepository.delete(rule);

            loggingService.logAction("Automated trade rule #" + rule.getAutomatedTradeRuleId() +
                    " executed and deactivated. Trade success: " + success);

            // Send notification to all active subscriptions for this user
            sendAutomatedTradeNotification(rule, triggeringCandle, success, tradeResult, transactionId);

        } catch (Exception e) {
            loggingService.logError("Error executing automated trade for rule #" + rule.getAutomatedTradeRuleId() +
                    ": " + e.getMessage(), e);

            // Send failure notification
            try {
                sendAutomatedTradeNotification(rule, triggeringCandle, false, null, null);
            } catch (Exception ne) {
                loggingService.logError("Error sending failure notification: " + ne.getMessage(), ne);
            }

            // Still delete the rule to prevent repeated failures
            automatedTradeRuleRepository.delete(rule);
        }
    }

    /**
     * Send notification about an automated trade execution
     */
    private void sendAutomatedTradeNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            TradeExecutionResponse tradeResult,
            Integer transactionId) {

        Integer userId = rule.getUser().getUserId();

        // Find all active subscriptions for this user
        for (AutomatedTradeSubscription subscription : activeSubscriptions.values()) {
            if (subscription.getUserId().equals(userId)) {
                AutomatedTradeNotificationResponse notification = new AutomatedTradeNotificationResponse();

                notification.setSuccess(success);
                notification.setAutomatedTradeRuleId(rule.getAutomatedTradeRuleId());
                notification.setPlatformName(rule.getPlatformStock().getPlatform().getPlatformName());
                notification.setStockSymbol(rule.getPlatformStock().getStock().getStockName());
                notification.setConditionType(rule.getConditionType().toString());
                notification.setThresholdValue(rule.getThresholdValue());
                notification.setActionType(rule.getActionType().toString());
                notification.setQuantityType(rule.getQuantityType().toString());
                notification.setQuantity(rule.getQuantity());
                notification.setCurrentPrice(triggeringCandle.getClosePrice());
                notification.setSubscriptionId(subscription.getId());
                notification.setTriggerTime(LocalDateTime.now());

                if (transactionId != null) {
                    notification.setTransactionId(transactionId);
                }

                // Set transaction status
                if (success) {
                    notification.setTransactionStatus("COMPLETED");
                } else {
                    notification.setTransactionStatus("FAILED");
                }

                // Create a descriptive message
                String message = formatAutomatedTradeMessage(rule, triggeringCandle, success, tradeResult);
                notification.setMessage(message);

                // Send the notification to the user
                simpMessagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/automated-trades",
                        notification
                );

                loggingService.logAction("Sent automated trade notification for rule #" + rule.getAutomatedTradeRuleId());
            }
        }
    }

    /**
     * Format a human-readable automated trade message
     */
    private String formatAutomatedTradeMessage(
            AutomatedTradeRule rule,
            MarketCandle candle,
            boolean success,
            TradeExecutionResponse tradeResult) {

        StringBuilder message = new StringBuilder();
        message.append("AUTOMATED TRADE ");

        if (success) {
            message.append("EXECUTED: ");
        } else {
            message.append("FAILED: ");
        }

        String conditionStr = formatConditionType(rule.getConditionType());
        String actionStr = formatActionType(rule.getActionType());
        String quantityTypeStr = formatQuantityType(rule.getQuantityType());

        // Strip trailing zeros from BigDecimal values
        String formattedThreshold = rule.getThresholdValue().stripTrailingZeros().toPlainString();
        String formattedCurrentPrice = candle.getClosePrice().stripTrailingZeros().toPlainString();
        String formattedQuantity = rule.getQuantity().stripTrailingZeros().toPlainString();

        message.append(rule.getPlatformStock().getPlatform().getPlatformName())
                .append(" ")
                .append(rule.getPlatformStock().getStock().getStockName())
                .append(" price ")
                .append(conditionStr)
                .append(" ")
                .append(formattedThreshold)
                .append(" (actual: ")
                .append(formattedCurrentPrice)
                .append(") triggered ")
                .append(actionStr)
                .append(" order for ")
                .append(formattedQuantity)
                .append(" (")
                .append(quantityTypeStr)
                .append(")");

        if (success && tradeResult != null) {
            message.append(" - Order executed successfully");
            if (tradeResult.getExecutedQty() != null) {
                String formattedExecutedQty = new BigDecimal(String.valueOf(tradeResult.getExecutedQty()))
                        .stripTrailingZeros()
                        .toPlainString();
                message.append(", executed quantity: ").append(formattedExecutedQty);
            }
            if (tradeResult.getCumulativeQuoteQty() != null) {
                String formattedCost = new BigDecimal(String.valueOf(tradeResult.getCumulativeQuoteQty()))
                        .stripTrailingZeros()
                        .toPlainString();
                message.append(", total cost: ").append(formattedCost);
            }
        } else {
            message.append(" - Failed to execute order");
            if (tradeResult != null && tradeResult.getStatus() != null) {
                message.append(" (Status: ").append(tradeResult.getStatus()).append(")");
            }
        }

        return message.toString();
    }

    /**
     * Format ConditionType enum to human-readable string
     */
    private String formatConditionType(AutomatedTradeRule.ConditionType conditionType) {
        switch (conditionType) {
            case PRICE_ABOVE:
                return "above";
            case PRICE_BELOW:
                return "below";
            default:
                return conditionType.toString().toLowerCase().replace("_", " ");
        }
    }

    /**
     * Format ActionType enum to human-readable string
     */
    private String formatActionType(AutomatedTradeRule.ActionType actionType) {
        switch (actionType) {
            case BUY:
                return "Buy";
            case SELL:
                return "Sell";
            default:
                return actionType.toString().charAt(0) + actionType.toString().substring(1).toLowerCase();
        }
    }

    /**
     * Format QuantityType enum to human-readable string
     */
    private String formatQuantityType(AutomatedTradeRule.QuantityType quantityType) {
        switch (quantityType) {
            case QUANTITY:
                return "Quantity";
            case QUOTE_ORDER_QTY:
                return "Quote Order Qty";
            default:
                return quantityType.toString().replace("_", " ");
        }
    }
}