package com.example.ehe_server.service.alert;

import com.example.ehe_server.dto.websocket.AlertNotificationResponse;
import com.example.ehe_server.dto.websocket.AlertSubscriptionResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.JwtRefreshToken;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.InvalidSubscriptionIdException;
import com.example.ehe_server.exception.custom.SubscriptionNotFoundException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.JwtRefreshTokenRepository;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class AlertWebSocketSubscriptionManager {

    private final UserContextService userContextService;
    private final JwtRefreshTokenRepository jwtRefreshTokenRepository;

    private static class AlertSubscription {
        private final String id;
        private final Integer userId;
        private final String destination;
        private boolean initialCheckCompleted;
        private LocalDateTime lastCheckedMinuteCandle;

        public AlertSubscription(String id, Integer userId, String destination) {
            this.id = id;
            this.userId = userId;
            this.destination = destination;
            this.initialCheckCompleted = false;
            this.lastCheckedMinuteCandle = null;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getDestination() {
            return destination;
        }

        public boolean isInitialCheckCompleted() {
            return initialCheckCompleted;
        }

        public LocalDateTime getLastCheckedMinuteCandle() {
            return lastCheckedMinuteCandle;
        }

        public void setInitialCheckCompleted(boolean initialCheckCompleted) {
            this.initialCheckCompleted = initialCheckCompleted;
        }

        public void setLastCheckedMinuteCandle(LocalDateTime lastCheckedMinuteCandle) {
            this.lastCheckedMinuteCandle = lastCheckedMinuteCandle;
        }
    }

    private final AlertRepository alertRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;
    private final Map<String, AlertSubscription> activeSubscriptions = new ConcurrentHashMap<>();

    public AlertWebSocketSubscriptionManager(
            AlertRepository alertRepository,
            MarketCandleRepository marketCandleRepository,
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            JwtRefreshTokenRepository jwtRefreshTokenRepository) {
        this.alertRepository = alertRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenRepository = jwtRefreshTokenRepository;
    }

    /**
     * Create a new subscription for alerts and return its ID
     */
    public AlertSubscriptionResponse createSubscription(Integer userId, String destination) {
        String subscriptionId = UUID.randomUUID().toString();

        AlertSubscription subscription = new AlertSubscription(
                subscriptionId,
                userId,
                destination);

        activeSubscriptions.put(subscriptionId, subscription);

        loggingService.logAction("Created alert subscription: " + subscriptionId);

        // Start the initial check process asynchronously
        new Thread(() -> performInitialAlertCheck(subscription)).start();

        return new AlertSubscriptionResponse(subscriptionId);
    }

    /**
     * Cancel a subscription
     */
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        AlertSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        loggingService.logAction("Cancelled alert subscription: " + subscriptionId);
    }

    /**
     * Perform the initial check for alerts, starting from the alert creation date
     */
    private void performInitialAlertCheck(AlertSubscription subscription) {
        try {
            // Get user's active alerts
            List<Alert> userAlerts = alertRepository.findByUser_UserIdAndActiveTrue(subscription.getUserId());

            if (userAlerts.isEmpty()) {
                loggingService.logAction("No active alerts found for initial check");
                subscription.setInitialCheckCompleted(true);
                return;
            }

            loggingService.logAction("Starting initial check for " + userAlerts.size() + " alerts");

            // Process each alert
            for (Alert alert : userAlerts) {
                // Get alert creation date, round up to the nearest minute, and adjust for UTC (-2 hours)
                LocalDateTime checkStartTime = alert.getDateCreated()
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plusMinutes(1)
                        .minusHours(2);

                PlatformStock platformStock = alert.getPlatformStock();

                loggingService.logAction("Checking alert #" + alert.getAlertId() + " for " +
                        platformStock.getPlatformName() + ":" + platformStock.getStockSymbol() +
                        " starting from " + checkStartTime);

                // Start checking with 1-minute candles
                checkAlertAgainstTimeframe(alert, platformStock, MarketCandle.Timeframe.M1,
                        checkStartTime, LocalDateTime.now(), subscription);
            }

            // Mark initial check as completed
            subscription.setInitialCheckCompleted(true);

            // Record last checked minute candle for future checks
            subscription.setLastCheckedMinuteCandle(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

            loggingService.logAction("Completed initial alert check for subscription: " + subscription.getId());

        } catch (Exception e) {
            e.printStackTrace();
            loggingService.logError("Error during initial alert check: " + e.getMessage(), e);
        }
    }

    /**
     * Check an alert against candles of a specific timeframe
     */
    private void checkAlertAgainstTimeframe(
            Alert alert,
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AlertSubscription subscription) {

        loggingService.logAction("Checking " + timeframe + " candles for alert #" + alert.getAlertId() +
                " from " + startTime + " to " + endTime);

        List<MarketCandle> candles = marketCandleRepository.findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                platformStock, timeframe, startTime, endTime);

        if (candles.isEmpty()) {
            loggingService.logAction("No " + timeframe + " candles found for the specified period");
            return;
        }

        loggingService.logAction("Found " + candles.size() + " " + timeframe + " candles to check");

        for (MarketCandle candle : candles) {
            // Check if alert condition is met
            boolean alertTriggered = checkAlertCondition(alert, candle);

            if (alertTriggered) {
                loggingService.logAction("Alert #" + alert.getAlertId() + " triggered on " + timeframe +
                        " candle at " + candle.getTimestamp());

                // Send alert notification
                sendAlertNotification(alert, candle, subscription);

                // Deactivate the alert (it's been triggered)
                alert.setActive(false);
                alertRepository.save(alert);

                loggingService.logAction("Alert #" + alert.getAlertId() + " deactivated after triggering");

                // Once triggered, no need to check further candles for this alert
                return;
            }
        }

        // Check if we should move to a higher timeframe
        if (!candles.isEmpty()) {
            LocalDateTime lastCandleTime = candles.get(candles.size() - 1).getTimestamp();

            // If we've reached a timeframe boundary, move to the next timeframe
            if (isTimeframeBoundary(lastCandleTime, timeframe)) {
                MarketCandle.Timeframe nextTimeframe = getNextTimeframe(timeframe);
                if (nextTimeframe != null) {
                    loggingService.logAction("Moving to higher timeframe " + nextTimeframe + " from " + lastCandleTime);

                    // Continue checking with the next higher timeframe
                    checkAlertAgainstTimeframe(alert, platformStock, nextTimeframe, lastCandleTime, endTime, subscription);
                }
            }
        }
    }

    /**
     * Check if an alert condition is met for a given candle
     */
    private boolean checkAlertCondition(Alert alert, MarketCandle candle) {
        BigDecimal priceToCheck;
        BigDecimal thresholdValue = alert.getThresholdValue();
        boolean result = false;

        // For price above alerts, check the high price
        // For price below alerts, check the low price
        if (alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE) {
            priceToCheck = candle.getHighPrice();
            result = priceToCheck.compareTo(thresholdValue) > 0;
        } else { // PRICE_BELOW
            priceToCheck = candle.getLowPrice();
            result = priceToCheck.compareTo(thresholdValue) < 0;
        }
        return result;
    }

    /**
     * Send a notification that an alert has been triggered
     */
    private void sendAlertNotification(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription) {
        AlertNotificationResponse notification = new AlertNotificationResponse();

        notification.setSuccess(true);
        notification.setAlertId(alert.getAlertId());
        notification.setPlatformName(alert.getPlatformStock().getPlatformName());
        notification.setStockSymbol(alert.getPlatformStock().getStockSymbol());
        notification.setConditionType(alert.getConditionType().toString());
        notification.setThresholdValue(alert.getThresholdValue());
        notification.setSubscriptionId(subscription.getId());
        notification.setTriggerTime(LocalDateTime.now());

        // Set the price that triggered the alert
        BigDecimal triggerPrice;
        if (alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE) {
            triggerPrice = triggeringCandle.getHighPrice();
        } else {
            triggerPrice = triggeringCandle.getLowPrice();
        }
        notification.setCurrentPrice(triggerPrice);

        // Create a descriptive message
        String message = formatAlertMessage(alert, triggeringCandle);
        notification.setMessage(message);

        // Send the notification to the client
        messagingTemplate.convertAndSendToUser(
                subscription.getUserId().toString(),
                "/queue/alerts", // Note: don't include "/user" here
                notification
        );
    }

    /**
     * Format a human-readable alert message
     */
    private String formatAlertMessage(Alert alert, MarketCandle candle) {
        String action = alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE ? "crossed above" : "dropped below";
        String symbol = alert.getPlatformStock().getStockSymbol();
        String platform = alert.getPlatformStock().getPlatformName();
        BigDecimal threshold = alert.getThresholdValue();
        BigDecimal triggerPrice = alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE ?
                candle.getHighPrice() : candle.getLowPrice();

        return "ALERT TRIGGERED: " + platform + " " + symbol + " price " + action + " " +
                threshold + " (actual: " + triggerPrice + ") at " + candle.getTimestamp();
    }

    /**
     * Check if a timestamp is at a boundary for moving to the next timeframe
     */
    private boolean isTimeframeBoundary(LocalDateTime time, MarketCandle.Timeframe timeframe) {
        switch (timeframe) {
            case M1:
                // 5-minute boundary: minute is divisible by 5
                return time.getMinute() % 5 == 0;
            case M5:
                // 15-minute boundary: minute is divisible by 15
                return time.getMinute() % 15 == 0;
            case M15:
                // 1-hour boundary: minute is 0
                return time.getMinute() == 0;
            case H1:
                // 4-hour boundary: hour is divisible by 4 and minute is 0
                return time.getHour() % 4 == 0 && time.getMinute() == 0;
            case H4:
                // 1-day boundary: hour is 0 and minute is 0
                return time.getHour() == 0 && time.getMinute() == 0;
            default:
                return false;
        }
    }

    /**
     * Get the next higher timeframe, or null if already at the highest
     */
    private MarketCandle.Timeframe getNextTimeframe(MarketCandle.Timeframe current) {
        switch (current) {
            case M1:
                return MarketCandle.Timeframe.M5;
            case M5:
                return MarketCandle.Timeframe.M15;
            case M15:
                return MarketCandle.Timeframe.H1;
            case H1:
                return MarketCandle.Timeframe.H4;
            case H4:
                return MarketCandle.Timeframe.D1;
            default:
                return null; // Already at highest timeframe
        }
    }

    /**
     * Validate active subscriptions and remove those without valid refresh tokens
     */
    private void validateAndCleanupSubscriptions() {
        List<String> subscriptionsToRemove = new ArrayList<>();

        for (AlertSubscription subscription : activeSubscriptions.values()) {
            List<JwtRefreshToken> userTokens = jwtRefreshTokenRepository.findByUser_UserId(subscription.getUserId());
            if (userTokens == null || userTokens.isEmpty()) {
                System.out.println("User " + subscription.getUserId() + " has no refresh tokens. Disconnecting subscription " + subscription.getId());
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
     * Scheduled task to check for new alerts every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkForNewAlerts() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);

        // Validate active subscriptions and remove those without refresh tokens
        validateAndCleanupSubscriptions();

        for (AlertSubscription subscription : activeSubscriptions.values()) {
            // Skip if initial check isn't completed yet
            if (!subscription.isInitialCheckCompleted()) {
                continue;
            }
            // Get the last checked minute
            LocalDateTime lastChecked = subscription.getLastCheckedMinuteCandle();

            // If this is the first check after initialization or we have a new minute
            if (lastChecked == null || currentMinute.isAfter(lastChecked)) {
                loggingService.logAction("Performing minute alert check at " + currentMinute);

                // Get user's active alerts
                List<Alert> activeAlerts = alertRepository.findByUser_UserIdAndActiveTrue(subscription.getUserId());

                if (!activeAlerts.isEmpty()) {
                    loggingService.logAction("Checking " + activeAlerts.size() + " active alerts for latest minute");

                    // For each active alert, check only the latest minute candle
                    for (Alert alert : activeAlerts) {
                        PlatformStock platformStock = alert.getPlatformStock();

                        // Look for the latest 1-minute candle
                        MarketCandle latestCandle = marketCandleRepository
                                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                                        platformStock, MarketCandle.Timeframe.M1);

                        if (latestCandle != null) {
                            if (lastChecked == null || latestCandle.getTimestamp().isAfter(lastChecked.minusHours(2))) {
                                loggingService.logAction("Found new candle at " + latestCandle.getTimestamp() + " for alert #" + alert.getAlertId());

                                // Check if alert condition is met
                                boolean alertTriggered = checkAlertCondition(alert, latestCandle);

                                if (alertTriggered) {
                                    loggingService.logAction("Alert #" + alert.getAlertId() + " triggered on latest candle");

                                    // Send alert notification
                                    sendAlertNotification(alert, latestCandle, subscription);

                                    // Deactivate the alert (it's been triggered)
                                    alert.setActive(false);
                                    alertRepository.save(alert);
                                }
                            }
                        }
                    }

                }
            }

            // Update the last checked minute
            subscription.setLastCheckedMinuteCandle(currentMinute);

        }
    }
}