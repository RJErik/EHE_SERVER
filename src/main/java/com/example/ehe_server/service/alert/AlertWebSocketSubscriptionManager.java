package com.example.ehe_server.service.alert;

import com.example.ehe_server.dto.websocket.AlertNotificationResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.AlertRepository;
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
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.alertRepository = alertRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
//        System.out.println("===== AlertWebSocketSubscriptionManager initialized =====");
        this.userContextService = userContextService;
    }

    /**
     * Create a new subscription for alerts and return its ID
     */
    public String createSubscription(Integer userId, String destination) {
        String subscriptionId = UUID.randomUUID().toString();

        AlertSubscription subscription = new AlertSubscription(
                subscriptionId,
                userId,
                destination);

        activeSubscriptions.put(subscriptionId, subscription);

        // Log subscription creation
//        System.out.println("===== CREATED NEW ALERT SUBSCRIPTION =====");
//        System.out.println("User ID: " + userId);
//        System.out.println("Subscription ID: " + subscriptionId);
//        System.out.println("Destination: " + destination);

        loggingService.logAction("Created alert subscription: " + subscriptionId);

        // Start the initial check process asynchronously
        new Thread(() -> performInitialAlertCheck(subscription)).start();

        return subscriptionId;
    }

    /**
     * Cancel a subscription
     */
    public boolean cancelSubscription(String subscriptionId) {
        AlertSubscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed != null) {
//            System.out.println("===== CANCELLED ALERT SUBSCRIPTION =====");
//            System.out.println("User ID: " + removed.getUserId());
//            System.out.println("Subscription ID: " + subscriptionId);

            loggingService.logAction("Cancelled alert subscription: " + subscriptionId);
            return true;
        }
        return false;
    }

    /**
     * Perform the initial check for alerts, starting from the alert creation date
     */
    private void performInitialAlertCheck(AlertSubscription subscription) {
        try {
//            System.out.println("\n===== STARTING INITIAL ALERT CHECK =====");
//            System.out.println("User ID: " + subscription.getUserId());
//            System.out.println("Subscription ID: " + subscription.getId());

            // Get user's active alerts
            List<Alert> userAlerts = alertRepository.findByUser_UserIdAndActiveTrue(subscription.getUserId());

            if (userAlerts.isEmpty()) {
//                System.out.println("No active alerts found for initial check");
                loggingService.logAction("No active alerts found for initial check");
                subscription.setInitialCheckCompleted(true);
                return;
            }

//            System.out.println("Found " + userAlerts.size() + " active alerts to check");
            loggingService.logAction("Starting initial check for " + userAlerts.size() + " alerts");

            // Process each alert
            for (Alert alert : userAlerts) {
//                System.out.println("\n----- Checking Alert ID: " + alert.getAlertId() + " -----");
//                System.out.println("Alert Type: " + alert.getConditionType());
//                System.out.println("Threshold Value: " + alert.getThresholdValue());
//                System.out.println("Stock: " + alert.getPlatformStock().getStockSymbol() + " on " + alert.getPlatformStock().getPlatformName());

                // Get alert creation date, round up to the nearest minute, and adjust for UTC (-2 hours)
                LocalDateTime checkStartTime = alert.getDateCreated()
                        .truncatedTo(ChronoUnit.MINUTES)
                        .plusMinutes(1)
                        .minusHours(2);

                PlatformStock platformStock = alert.getPlatformStock();

//                System.out.println("Check start time: " + checkStartTime);
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

//            System.out.println("\n===== COMPLETED INITIAL ALERT CHECK =====");
//            System.out.println("User ID: " + subscription.getUserId());
//            System.out.println("Subscription ID: " + subscription.getId());
//            System.out.println("Last checked candle time: " + subscription.getLastCheckedMinuteCandle());

            loggingService.logAction("Completed initial alert check for subscription: " + subscription.getId());

        } catch (Exception e) {
//            System.out.println("\n===== ERROR DURING INITIAL ALERT CHECK =====");
//            System.out.println("User ID: " + subscription.getUserId());
//            System.out.println("Subscription ID: " + subscription.getId());
//            System.out.println("Error: " + e.getMessage());
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

        try {
//            System.out.println("\n----- Checking Timeframe: " + timeframe + " -----");
//            System.out.println("Alert ID: " + alert.getAlertId());
//            System.out.println("Period: " + startTime + " to " + endTime);

            loggingService.logAction("Checking " + timeframe + " candles for alert #" + alert.getAlertId() +
                    " from " + startTime + " to " + endTime);

            List<MarketCandle> candles = marketCandleRepository.findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                    platformStock, timeframe, startTime, endTime);

            if (candles.isEmpty()) {
//                System.out.println("No " + timeframe + " candles found for the specified period");
                loggingService.logAction("No " + timeframe + " candles found for the specified period");
                return;
            }

//            System.out.println("Found " + candles.size() + " " + timeframe + " candles to check");
            loggingService.logAction("Found " + candles.size() + " " + timeframe + " candles to check");

            for (MarketCandle candle : candles) {
//                System.out.println("\nChecking candle at time: " + candle.getTimestamp());
//                System.out.println("Candle Open: " + candle.getOpenPrice() + ", High: " + candle.getHighPrice() +
//                        ", Low: " + candle.getLowPrice() + ", Close: " + candle.getClosePrice());

                // Check if alert condition is met
                boolean alertTriggered = checkAlertCondition(alert, candle);

//                System.out.println("Alert triggered: " + alertTriggered);

                if (alertTriggered) {
//                    System.out.println("===== ALERT TRIGGERED! =====");
//                    System.out.println("Alert ID: " + alert.getAlertId());
//                    System.out.println("Candle time: " + candle.getTimestamp());
//                    System.out.println("Stock: " + platformStock.getStockSymbol());

                    loggingService.logAction("Alert #" + alert.getAlertId() + " triggered on " + timeframe +
                            " candle at " + candle.getTimestamp());

                    // Send alert notification
                    sendAlertNotification(alert, candle, subscription);

                    // Deactivate the alert (it's been triggered)
                    alert.setActive(false);
                    alertRepository.save(alert);

//                    System.out.println("Alert deactivated after triggering");
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
//                        System.out.println("Moving to higher timeframe " + nextTimeframe + " from " + lastCandleTime);
                        loggingService.logAction("Moving to higher timeframe " + nextTimeframe + " from " + lastCandleTime);

                        // Continue checking with the next higher timeframe
                        checkAlertAgainstTimeframe(alert, platformStock, nextTimeframe, lastCandleTime, endTime, subscription);
                    }
                }
            }

        } catch (Exception e) {
//            System.out.println("ERROR checking alert against timeframe " + timeframe + ": " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error checking alert against timeframe " + timeframe + ": " + e.getMessage(), e);
        }
    }

    /**
     * Check if an alert condition is met for a given candle
     */
    private boolean checkAlertCondition(Alert alert, MarketCandle candle) {
        BigDecimal priceToCheck;
        BigDecimal thresholdValue = alert.getThresholdValue();
        boolean result = false;

//        System.out.println("----- Checking Alert Condition -----");
//        System.out.println("Alert ID: " + alert.getAlertId());
//        System.out.println("Condition Type: " + alert.getConditionType());
//        System.out.println("Threshold Value: " + thresholdValue);

        // For price above alerts, check the high price
        // For price below alerts, check the low price
        if (alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE) {
            priceToCheck = candle.getHighPrice();
//            System.out.println("Checking HIGH price: " + priceToCheck + " > " + thresholdValue + "?");
            result = priceToCheck.compareTo(thresholdValue) > 0;
        } else { // PRICE_BELOW
            priceToCheck = candle.getLowPrice();
//            System.out.println("Checking LOW price: " + priceToCheck + " < " + thresholdValue + "?");
            result = priceToCheck.compareTo(thresholdValue) < 0;
        }

//        System.out.println("Condition met: " + result);
        return result;
    }

    /**
     * Send a notification that an alert has been triggered
     */
    private void sendAlertNotification(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription) {
//        System.out.println("\n===== SENDING ALERT NOTIFICATION =====");
//        System.out.println("Alert ID: " + alert.getAlertId());
//        System.out.println("User ID: " + subscription.getUserId());
//        System.out.println("Subscription ID: " + subscription.getId());
//        System.out.println("Destination: " + subscription.getDestination());

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

//        System.out.println("Trigger price: " + triggerPrice);

        // Create a descriptive message
        String message = formatAlertMessage(alert, triggeringCandle);
        notification.setMessage(message);
//        System.out.println("Alert message: " + message);

        // Send the notification to the client
        messagingTemplate.convertAndSendToUser(
                subscription.getUserId().toString(),
                "/queue/alerts", // Note: don't include "/user" here
                notification
        );
//        System.out.println("Notification sent successfully");
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
     * Scheduled task to check for new alerts every minute
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkForNewAlerts() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);

//        System.out.println("\n\n===== RUNNING SCHEDULED ALERT CHECK =====");
//        System.out.println("Check time: " + now);
//        System.out.println("Current minute: " + currentMinute);
//        System.out.println("Active subscriptions: " + activeSubscriptions.size());

        for (AlertSubscription subscription : activeSubscriptions.values()) {
            // Skip if initial check isn't completed yet
            if (!subscription.isInitialCheckCompleted()) {
//                System.out.println("Skipping subscription " + subscription.getId() + " - initial check not completed yet");
                continue;
            }

            try {
//                System.out.println("\n----- Processing Subscription -----");
//                System.out.println("Subscription ID: " + subscription.getId());
//                System.out.println("User ID: " + subscription.getUserId());

                // Get the last checked minute
                LocalDateTime lastChecked = subscription.getLastCheckedMinuteCandle();
//                System.out.println("Last checked: " + lastChecked);

                // If this is the first check after initialization or we have a new minute
                if (lastChecked == null || currentMinute.isAfter(lastChecked)) {
//                    System.out.println("Performing minute alert check at " + currentMinute);
                    loggingService.logAction("Performing minute alert check at " + currentMinute);

                    // Get user's active alerts
                    List<Alert> activeAlerts = alertRepository.findByUser_UserIdAndActiveTrue(subscription.getUserId());
//                    System.out.println("Found " + activeAlerts.size() + " active alerts");

                    if (!activeAlerts.isEmpty()) {
//                        System.out.println("Checking " + activeAlerts.size() + " active alerts for latest minute");
                        loggingService.logAction("Checking " + activeAlerts.size() + " active alerts for latest minute");

                        // For each active alert, check only the latest minute candle
                        for (Alert alert : activeAlerts) {
                            PlatformStock platformStock = alert.getPlatformStock();

//                            System.out.println("\n----- Checking Alert ID: " + alert.getAlertId() + " -----");
//                            System.out.println("Stock: " + platformStock.getStockSymbol() + " on " + platformStock.getPlatformName());
//                            System.out.println("Condition: " + alert.getConditionType() + " " + alert.getThresholdValue());

                            // Look for the latest 1-minute candle
                            MarketCandle latestCandle = marketCandleRepository
                                    .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                                            platformStock, MarketCandle.Timeframe.M1);

                            if (latestCandle != null) {
//                                System.out.println("Latest candle found at: " + latestCandle.getTimestamp());
//                                System.out.println("Candle data: Open=" + latestCandle.getOpenPrice() +
//                                        ", High=" + latestCandle.getHighPrice() +
//                                        ", Low=" + latestCandle.getLowPrice() +
//                                        ", Close=" + latestCandle.getClosePrice());

                                if (lastChecked == null || latestCandle.getTimestamp().isAfter(lastChecked.minusHours(2))) {
//                                    System.out.println("Found new candle at " + latestCandle.getTimestamp() + " for alert #" + alert.getAlertId());
                                    loggingService.logAction("Found new candle at " + latestCandle.getTimestamp() + " for alert #" + alert.getAlertId());

                                    // Check if alert condition is met
                                    boolean alertTriggered = checkAlertCondition(alert, latestCandle);

                                    if (alertTriggered) {
//                                        System.out.println("ALERT TRIGGERED on latest candle!");
                                        loggingService.logAction("Alert #" + alert.getAlertId() + " triggered on latest candle");

                                        // Send alert notification
                                        sendAlertNotification(alert, latestCandle, subscription);

                                        // Deactivate the alert (it's been triggered)
                                        alert.setActive(false);
                                        alertRepository.save(alert);
//                                        System.out.println("Alert deactivated after triggering");
                                    } else {
//                                        System.out.println("Alert condition not met");
                                    }
                                } else {
//                                    System.out.println("Latest candle already checked previously");
                                }
                            } else {
//                                System.out.println("No candles found for this stock");
                            }
                        }
                    }

                    // Update the last checked minute
                    subscription.setLastCheckedMinuteCandle(currentMinute);
//                    System.out.println("Updated last checked to: " + currentMinute);
                } else {
//                    System.out.println("No new candles since last check, skipping");
                }
            } catch (Exception e) {
//                System.out.println("ERROR during periodic alert check: " + e.getMessage());
                e.printStackTrace();
                loggingService.logError("Error during periodic alert check: " + e.getMessage(), e);
            }
        }
//        System.out.println("===== SCHEDULED ALERT CHECK COMPLETED =====\n");
    }
}
