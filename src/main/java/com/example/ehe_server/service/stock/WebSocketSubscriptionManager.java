package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse;
import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.CandleUpdateMessage;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.MarketCandleServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class WebSocketSubscriptionManager {

    private final UserContextService userContextService;

    private static class Subscription {
        private final String id;
        private String platformName;
        private String stockSymbol;
        private String timeframe;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private final String destination;
        private LocalDateTime lastUpdateTime;
        private String subscriptionType; // Added field

        // Track the latest candle for modification detection
        private LocalDateTime latestCandleTimestamp;
        private BigDecimal latestCandleOpen;
        private BigDecimal latestCandleHigh;
        private BigDecimal latestCandleLow;
        private BigDecimal latestCandleClose;
        private BigDecimal latestCandleVolume;

        public Subscription(
                String id,
                String platformName,
                String stockSymbol,
                String timeframe,
                LocalDateTime startDate,
                LocalDateTime endDate,
                String destination,
                String subscriptionType) { // Updated constructor
            this.id = id;
            this.platformName = platformName;
            this.stockSymbol = stockSymbol;
            this.timeframe = timeframe;
            this.startDate = startDate;
            this.endDate = endDate;
            this.destination = destination;
            this.subscriptionType = subscriptionType; // Store the subscription type
            this.lastUpdateTime = LocalDateTime.now();
        }

        // Getters
        public String getId() { return id; }
        public String getPlatformName() { return platformName; }
        public String getStockSymbol() { return stockSymbol; }
        public String getTimeframe() { return timeframe; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public String getDestination() { return destination; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public String getSubscriptionType() { return subscriptionType; } // Getter

        public LocalDateTime getLatestCandleTimestamp() { return latestCandleTimestamp; }
        public BigDecimal getLatestCandleOpen() { return latestCandleOpen; }
        public BigDecimal getLatestCandleHigh() { return latestCandleHigh; }
        public BigDecimal getLatestCandleLow() { return latestCandleLow; }
        public BigDecimal getLatestCandleClose() { return latestCandleClose; }
        public BigDecimal getLatestCandleVolume() { return latestCandleVolume; }

        public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public void updateTimeRange(LocalDateTime newStartDate, LocalDateTime newEndDate) {
            if (newStartDate != null) {
                this.startDate = newStartDate;
            }
            if (newEndDate != null) {
                this.endDate = newEndDate;
            }
        }

        public void updateSubscriptionType(String newSubscriptionType) {
            if (newSubscriptionType != null) {
                this.subscriptionType = newSubscriptionType;
            }
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

    private final MarketCandleServiceInterface marketCandleService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;
    private final Map<String, Subscription> activeSubscriptions = new ConcurrentHashMap<>();

    public WebSocketSubscriptionManager(
            MarketCandleServiceInterface marketCandleService,
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.marketCandleService = marketCandleService;
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    /**
     * Create a new subscription and return its ID
     */
    public String createSubscription(
            String platformName,
            String stockSymbol,
            String timeframe,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String destination,
            String subscriptionType) {
        userContextService.setUser("SYSTEM", "SYSTEM");

        String subscriptionId = UUID.randomUUID().toString();

        Subscription subscription = new Subscription(
                subscriptionId,
                platformName,
                stockSymbol,
                timeframe,
                startDate,
                endDate,
                destination,
                subscriptionType);

        activeSubscriptions.put(subscriptionId, subscription);

        // Log subscription creation
        loggingService.logAction("Created candle data subscription: " + subscriptionId +
                        " for " + platformName + ":" + stockSymbol + " " + timeframe +
                        (subscriptionType != null ? " (Type: " + subscriptionType + ")" : ""));

        // Send initial data
        sendInitialData(subscription);

        return subscriptionId;
    }

    /**
     * Cancel a subscription
     */
    public boolean cancelSubscription(String subscriptionId) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        Subscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed != null) {
            loggingService.logAction("Cancelled candle data subscription: " + subscriptionId);
            return true;
        }
        return false;
    }

    /**
     * Update a subscription's time range and type
     */
    public boolean updateSubscription(
            String subscriptionId,
            LocalDateTime newStartDate,
            LocalDateTime newEndDate,
            boolean resetData,
            String newSubscriptionType) { // Added parameter
        userContextService.setUser("SYSTEM", "SYSTEM");

        Subscription subscription = activeSubscriptions.get(subscriptionId);
        if (subscription == null) {
            return false;
        }

        // Update time range
        subscription.updateTimeRange(newStartDate, newEndDate);

        // Update subscription type if provided
        if (newSubscriptionType != null) {
            subscription.updateSubscriptionType(newSubscriptionType);
        }

        // Log update
        loggingService.logAction("Updated subscription " + subscriptionId + " time range to " +
                        subscription.getStartDate() + " - " + subscription.getEndDate() +
                        (newSubscriptionType != null ? " and type to " + newSubscriptionType : ""));

        // If requested, reset data and send fresh data
        if (resetData) {
            sendInitialData(subscription);
        }

        return true;
    }

    /**
     * Legacy method to maintain backward compatibility
     */
    public boolean updateSubscription(
            String subscriptionId,
            LocalDateTime newStartDate,
            LocalDateTime newEndDate,
            boolean resetData) {

        return updateSubscription(subscriptionId, newStartDate, newEndDate, resetData, null);
    }

    /**
     * Get subscription type for a given ID
     */
    public String getSubscriptionType(String subscriptionId) {
        Subscription subscription = activeSubscriptions.get(subscriptionId);
        return subscription != null ? subscription.getSubscriptionType() : null;
    }

    /**
     * Send initial data for a new subscription
     */
    private void sendInitialData(Subscription subscription) {
        try {
            CandleDataResponse response = marketCandleService.getCandleData(
                    subscription.getPlatformName(),
                    subscription.getStockSymbol(),
                    subscription.getTimeframe(),
                    subscription.getStartDate(),
                    subscription.getEndDate());

            // Set the subscription ID and type in the response
            response.setSubscriptionId(subscription.getId());
            response.setSubscriptionType(subscription.getSubscriptionType());

            // If successful, update the latest candle info
            if (response.isSuccess() && response.getCandles() != null && !response.getCandles().isEmpty()) {
                // Update with the last candle in the list
                subscription.updateLatestCandle(response.getCandles().get(response.getCandles().size() - 1));
            }

            // Send to the subscription destination
            messagingTemplate.convertAndSend(subscription.getDestination(), response);

        } catch (Exception e) {
            loggingService.logError("Error sending initial data for subscription " +
                            subscription.getId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to check for updates and send them to clients
     * Runs every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void checkForUpdatesAndSendHeartbeats() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        LocalDateTime now = LocalDateTime.now();

        activeSubscriptions.values().forEach(subscription -> {
            try {
                List<CandleDataResponse.CandleData> updatedCandles = new ArrayList<>();
                boolean hasUpdates = false;

                // Check for new candles first
                List<CandleDataResponse.CandleData> newCandles =
                        marketCandleService.getUpdatedCandles(
                                subscription.getPlatformName(),
                                subscription.getStockSymbol(),
                                subscription.getTimeframe(),
                                subscription.getStartDate(),
                                subscription.getEndDate(),
                                subscription.getLastUpdateTime());

                if (!newCandles.isEmpty()) {
                    updatedCandles.addAll(newCandles);
                    hasUpdates = true;

                    // Update the latest candle info with the most recent candle
                    subscription.updateLatestCandle(newCandles.get(newCandles.size() - 1));
                }

                // For non-1-minute timeframes, also check if the latest candle was modified
                if (!marketCandleService.isOneMinuteTimeframe(subscription.getTimeframe()) &&
                        subscription.getLatestCandleTimestamp() != null) {

                    CandleData modifiedCandle = marketCandleService.getModifiedLatestCandle(
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
                        // Only add if not already included in new candles
                        if (updatedCandles.stream().noneMatch(c ->
                                c.getTimestamp().equals(modifiedCandle.getTimestamp()))) {
                            updatedCandles.add(modifiedCandle);
                            hasUpdates = true;
                        }

                        // Update the latest candle info
                        subscription.updateLatestCandle(modifiedCandle);
                    }
                }

                // Create update message
                CandleUpdateMessage updateMessage = new CandleUpdateMessage();
                updateMessage.setSubscriptionId(subscription.getId());
                updateMessage.setSubscriptionType(subscription.getSubscriptionType()); // Include subscription type
                updateMessage.setUpdateTimestamp(now);

                // If there are updates, send them
                if (hasUpdates) {
                    updateMessage.setUpdateType("UPDATE");
                    updateMessage.setUpdatedCandles(updatedCandles);
                    messagingTemplate.convertAndSend(subscription.getDestination(), updateMessage);
                    subscription.setLastUpdateTime(now);

                    loggingService.logAction("Sent " + updatedCandles.size() + " candle updates for subscription " +
                                    subscription.getId() +
                                    (subscription.getSubscriptionType() != null ?
                                            " (Type: " + subscription.getSubscriptionType() + ")" : ""));
                } else {
                    // Send heartbeat every 10 seconds even if there are no updates
                    updateMessage.setUpdateType("HEARTBEAT");
                    updateMessage.setUpdatedCandles(new ArrayList<>());
                    messagingTemplate.convertAndSend(subscription.getDestination(), updateMessage);
                }

            } catch (Exception e) {
                loggingService.logError("Error checking updates for subscription " +
                                subscription.getId() + ": " + e.getMessage(), e);
            }
        });
    }
}
