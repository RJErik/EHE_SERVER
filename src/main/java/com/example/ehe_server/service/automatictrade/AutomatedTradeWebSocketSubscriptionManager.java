package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.dto.websocket.AutomatedTradeNotificationResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutomatedTradeWebSocketSubscriptionManager {

    private static class AutomatedTradeSubscription {
        private final String id;
        private final Integer userId;
        private final String destination;

        public AutomatedTradeSubscription(String id, Integer userId, String destination) {
            this.id = id;
            this.userId = userId;
            this.destination = destination;
        }

        // Getters
        public String getId() { return id; }
        public Integer getUserId() { return userId; }
        public String getDestination() { return destination; }
    }

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;
    private final TradingServiceInterface tradingService;
    private final TransactionRepository transactionRepository;
    private final AuditContextService auditContextService;
    private final Map<String, AutomatedTradeSubscription> activeSubscriptions = new ConcurrentHashMap<>();

    public AutomatedTradeWebSocketSubscriptionManager(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            MarketCandleRepository marketCandleRepository,
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService,
            TradingServiceInterface tradingService,
            TransactionRepository transactionRepository,
            AuditContextService auditContextService) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
        this.tradingService = tradingService;
        this.transactionRepository = transactionRepository;
        this.auditContextService = auditContextService;
    }

    /**
     * Create a new subscription for automated trades and return its ID
     */
    public String createSubscription(Integer userId, String destination) {
        String subscriptionId = UUID.randomUUID().toString();

        AutomatedTradeSubscription subscription = new AutomatedTradeSubscription(
                subscriptionId,
                userId,
                destination);

        activeSubscriptions.put(subscriptionId, subscription);

        loggingService.logAction(userId, userId.toString(),
                "Created automated trade subscription: " + subscriptionId);

        return subscriptionId;
    }

    /**
     * Cancel a subscription
     */
    public boolean cancelSubscription(String subscriptionId) {
        AutomatedTradeSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed != null) {
            loggingService.logAction(removed.getUserId(), removed.getUserId().toString(),
                    "Cancelled automated trade subscription: " + subscriptionId);
            return true;
        }
        return false;
    }

    /**
     * Scheduled task to check for automated trade rules every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkAutomatedTradeRules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);

        loggingService.logAction(null, "system", "Checking automated trade rules at " + currentMinute);

        try {
            // Get all active automated trade rules
            List<AutomatedTradeRule> activeRules = automatedTradeRuleRepository.findAllByIsActiveTrue();

            if (activeRules.isEmpty()) {
                return;
            }

            for (AutomatedTradeRule rule : activeRules) {
                processRule(rule, currentMinute);
            }
        } catch (Exception e) {
            loggingService.logError(null, "system",
                    "Error in automated trade rule check: " + e.getMessage(), e);
        }
    }

    /**
     * Process a single automated trade rule
     */
    private void processRule(AutomatedTradeRule rule, LocalDateTime currentTime) {
        try {
            PlatformStock platformStock = rule.getPlatformStock();

            // Look for the latest 1-minute candle
            MarketCandle latestCandle = marketCandleRepository
                    .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            platformStock, MarketCandle.Timeframe.M1);

            if (latestCandle == null) {
                loggingService.logAction(rule.getUser().getUserId(), rule.getUser().getUserId().toString(),
                        "No candle data found for " + platformStock.getStockSymbol() + " when checking rule #" + rule.getAutomatedTradeRuleId());
                return;
            }

            // Check if rule condition is triggered
            boolean isTriggered = checkRuleCondition(rule, latestCandle);

            if (isTriggered) {
                // Execute the trade
                executeAutomatedTrade(rule, latestCandle);
            }
        } catch (Exception e) {
            loggingService.logError(rule.getUser().getUserId(), rule.getUser().getUserId().toString(),
                    "Error processing automated trade rule #" + rule.getAutomatedTradeRuleId() + ": " + e.getMessage(), e);
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
        String userIdStr = String.valueOf(userId);

        auditContextService.setCurrentUser(userIdStr);

        try {
            loggingService.logAction(userId, userIdStr,
                    "Executing automated trade for rule #" + rule.getAutomatedTradeRuleId());

            // Determine action parameters
            Integer portfolioId = rule.getPortfolio().getPortfolioId();
            String stockSymbol = rule.getPlatformStock().getStockSymbol();
            String action = rule.getActionType().toString();
            BigDecimal quantity = rule.getQuantity();
            String quantityType = rule.getQuantityType().toString();

            // Execute the market order
            Map<String, Object> tradingResult = tradingService.executeMarketOrder(
                    portfolioId, stockSymbol, action, quantity, quantityType, rule.getUser().getUserId());

            boolean success = (boolean) tradingResult.getOrDefault("success", false);

            // Extract transaction ID if available
            Integer transactionId = null;
            if (success && tradingResult.get("order") instanceof Map) {
                Map<String, Object> orderDetails = (Map<String, Object>) tradingResult.get("order");
                if (orderDetails.containsKey("orderId")) {
                    // This is just the exchange order ID, not our internal transaction ID
                    // We would need to find our transaction based on other criteria

                    // For this example, we'll just use the latest transaction for this portfolio
                    List<Transaction> transactions = transactionRepository.findByPortfolio_PortfolioId(portfolioId);
                    if (!transactions.isEmpty()) {
                        // Sort by transaction date descending to get the most recent
                        transactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));
                        transactionId = transactions.get(0).getTransactionId();
                    }
                }
            }

            // Deactivate the rule after execution regardless of success
            rule.setActive(false);
            automatedTradeRuleRepository.save(rule);

            loggingService.logAction(userId, userIdStr,
                    "Automated trade rule #" + rule.getAutomatedTradeRuleId() +
                            " executed and deactivated. Trade success: " + success);

            // Send notification to all active subscriptions for this user
            sendAutomatedTradeNotification(rule, triggeringCandle, success, tradingResult, transactionId);

        } catch (Exception e) {
            loggingService.logError(userId, userIdStr,
                    "Error executing automated trade for rule #" + rule.getAutomatedTradeRuleId() + ": " + e.getMessage(), e);

            // Send failure notification
            try {
                sendAutomatedTradeNotification(rule, triggeringCandle, false,
                        Map.of("success", false, "message", e.getMessage()), null);
            } catch (Exception ne) {
                loggingService.logError(userId, userIdStr,
                        "Error sending failure notification: " + ne.getMessage(), ne);
            }

            // Still deactivate the rule to prevent repeated failures
            rule.setActive(false);
            automatedTradeRuleRepository.save(rule);
        }
    }

    /**
     * Send notification about an automated trade execution
     */
    private void sendAutomatedTradeNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            Map<String, Object> tradingResult,
            Integer transactionId) {

        Integer userId = rule.getUser().getUserId();

        // Find all active subscriptions for this user
        for (AutomatedTradeSubscription subscription : activeSubscriptions.values()) {
            if (subscription.getUserId().equals(userId)) {
                AutomatedTradeNotificationResponse notification = new AutomatedTradeNotificationResponse();

                notification.setSuccess(success);
                notification.setAutomatedTradeRuleId(rule.getAutomatedTradeRuleId());
                notification.setPlatformName(rule.getPlatformStock().getPlatformName());
                notification.setStockSymbol(rule.getPlatformStock().getStockSymbol());
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
                String message = formatAutomatedTradeMessage(rule, triggeringCandle, success, tradingResult);
                notification.setMessage(message);

                // Send the notification to the user
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/automated-trades",
                        notification
                );

                loggingService.logAction(userId, userId.toString(),
                        "Sent automated trade notification for rule #" + rule.getAutomatedTradeRuleId());
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
            Map<String, Object> tradingResult) {

        StringBuilder message = new StringBuilder();
        message.append("AUTOMATED TRADE ");

        if (success) {
            message.append("EXECUTED: ");
        } else {
            message.append("FAILED: ");
        }

        String conditionStr = rule.getConditionType() == AutomatedTradeRule.ConditionType.PRICE_ABOVE
                ? "above" : "below";

        message.append(rule.getPlatformStock().getPlatformName())
                .append(" ")
                .append(rule.getPlatformStock().getStockSymbol())
                .append(" price ")
                .append(conditionStr)
                .append(" ")
                .append(rule.getThresholdValue())
                .append(" (actual: ")
                .append(candle.getClosePrice())
                .append(") triggered ")
                .append(rule.getActionType())
                .append(" order for ")
                .append(rule.getQuantity())
                .append(" (")
                .append(rule.getQuantityType())
                .append(")");

        if (success) {
            message.append(" - Order executed successfully");
            if (tradingResult.containsKey("order")) {
                Map<String, Object> orderDetails = (Map<String, Object>) tradingResult.get("order");
                if (orderDetails.containsKey("executedQty")) {
                    message.append(", executed quantity: ").append(orderDetails.get("executedQty"));
                }
                if (orderDetails.containsKey("cummulativeQuoteQty")) {
                    message.append(", total cost: ").append(orderDetails.get("cummulativeQuoteQty"));
                }
            }
        } else {
            message.append(" - Failed to execute order");
            if (tradingResult.containsKey("message")) {
                message.append(": ").append(tradingResult.get("message"));
            }
        }

        return message.toString();
    }
}