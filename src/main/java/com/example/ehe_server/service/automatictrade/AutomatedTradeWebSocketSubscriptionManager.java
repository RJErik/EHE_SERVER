package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.dto.TradeExecutionResponse;
import com.example.ehe_server.dto.websocket.AutomatedTradeNotificationResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.TransactionRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.trade.TradingServiceInterface;
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
@Transactional
public class AutomatedTradeWebSocketSubscriptionManager {

    private final UserContextService userContextService;

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
        public String getId() {
            return id;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getDestination() {
            return destination;
        }
    }

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final LoggingServiceInterface loggingService;
    private final TradingServiceInterface tradingService;
    private final TransactionRepository transactionRepository;
    private final Map<String, AutomatedTradeSubscription> activeSubscriptions = new ConcurrentHashMap<>();

    public AutomatedTradeWebSocketSubscriptionManager(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            MarketCandleRepository marketCandleRepository,
            SimpMessagingTemplate simpMessagingTemplate,
            LoggingServiceInterface loggingService,
            TradingServiceInterface tradingService,
            TransactionRepository transactionRepository,
            UserContextService userContextService) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.loggingService = loggingService;
        this.tradingService = tradingService;
        this.transactionRepository = transactionRepository;
        this.userContextService = userContextService;
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

        loggingService.logAction("Created automated trade subscription: " + subscriptionId);

        return subscriptionId;
    }

    /**
     * Cancel a subscription
     */
    public boolean cancelSubscription(String subscriptionId) {
        AutomatedTradeSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed != null) {
            loggingService.logAction("Cancelled automated trade subscription: " + subscriptionId);
            return true;
        }
        return false;
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

        try {
            // Get all active automated trade rules
            List<AutomatedTradeRule> activeRules = automatedTradeRuleRepository.findAllByIsActiveTrue();

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
                loggingService.logAction("No candle data found for " + platformStock.getStockSymbol() +
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
            String stockSymbol = rule.getPlatformStock().getStockSymbol();
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
                    transactionId = transactions.get(0).getTransactionId();
                }
            }

            // Deactivate the rule after execution regardless of success
            rule.setActive(false);
            automatedTradeRuleRepository.save(rule);

            loggingService.logAction("Automated trade rule #" + rule.getAutomatedTradeRuleId() +
                    " executed and deactivated. Trade success: ");

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
            TradeExecutionResponse tradeResult,
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

        if (success && tradeResult != null) {
            message.append(" - Order executed successfully");
            if (tradeResult.getExecutedQty() != null) {
                message.append(", executed quantity: ").append(tradeResult.getExecutedQty());
            }
            if (tradeResult.getCummulativeQuoteQty() != null) {
                message.append(", total cost: ").append(tradeResult.getCummulativeQuoteQty());
            }
            if (tradeResult.getOrderId() != null) {
                message.append(", order ID: ").append(tradeResult.getOrderId());
            }
        } else {
            message.append(" - Failed to execute order");
            if (tradeResult != null && tradeResult.getStatus() != null) {
                message.append(" (Status: ").append(tradeResult.getStatus()).append(")");
            }
        }

        return message.toString();
    }
}