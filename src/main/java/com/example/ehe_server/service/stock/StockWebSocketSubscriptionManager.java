package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse;
import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.CandleUpdateMessage;
import com.example.ehe_server.dto.websocket.StockCandleSubscriptionResponse;
import com.example.ehe_server.dto.websocket.StockCandleUpdateSubscriptionResponse;
import com.example.ehe_server.exception.custom.InvalidSubscriptionIdException;
import com.example.ehe_server.exception.custom.MissingStockSubscriptionParametersException;
import com.example.ehe_server.exception.custom.SubscriptionNotFoundException;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.MarketCandleServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class StockWebSocketSubscriptionManager {

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
            this.lastUpdateTime = LocalDateTime.now(ZoneOffset.UTC);
        }

        // Getters
        public String getId() {
            return id;
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

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }

        public String getDestination() {
            return destination;
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public String getSubscriptionType() {
            return subscriptionType;
        } // Getter

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

    public StockWebSocketSubscriptionManager(
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
    public StockCandleSubscriptionResponse createSubscription(
            String platformName,
            String stockSymbol,
            String timeframe,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String destination,
            String subscriptionType) {

        String subscriptionId = UUID.randomUUID().toString();

        // Validate request
        List<String> missingParams = new ArrayList<>();
        if (platformName == null || platformName.trim().isEmpty()) {
            missingParams.add("platformName");
        }
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            missingParams.add("stockSymbol");
        }
        if (timeframe == null || timeframe.trim().isEmpty()) {
            missingParams.add("timeframe");
        }
        if (startDate == null) {
            missingParams.add("startDate");
        }
        if (endDate == null) {
            missingParams.add("endDate");
        }

        if (!missingParams.isEmpty()) {
            String missingParamsStr = String.join(", ", missingParams);
            throw new MissingStockSubscriptionParametersException(missingParamsStr);
        }

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

        System.out.println("Subscriptions length in creation:" + activeSubscriptions.size());

        for (Subscription subscription1 : activeSubscriptions.values()) {
            System.out.println("Subscription id" + subscription1.id);
        }

        // Log subscription creation
        loggingService.logAction("Created candle data subscription: " + subscriptionId +
                " for " + platformName + ":" + stockSymbol + " " + timeframe +
                (subscriptionType != null ? " (Type: " + subscriptionType + ")" : ""));

        // Send initial data
        sendInitialData(subscription);

        return new StockCandleSubscriptionResponse(subscriptionId, subscriptionType);
    }

    /**
     * Cancel a subscription
     */
    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }
        Subscription removed = activeSubscriptions.remove(subscriptionId);
        if (removed == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
        }

        System.out.println("Subscription id being removed:" + removed.id);

        loggingService.logAction("Cancelled alert subscription: " + subscriptionId);
    }

    /**
     * Update a subscription's time range and type
     */
    public StockCandleUpdateSubscriptionResponse updateSubscription(
            String subscriptionId,
            LocalDateTime newStartDate,
            LocalDateTime newEndDate,
            boolean resetData,
            String newSubscriptionType) { // Added parameter

        // Validate request
        if (subscriptionId == null) {
            throw new InvalidSubscriptionIdException();
        }

        Subscription subscription = activeSubscriptions.get(subscriptionId);
        if (subscription == null) {
            throw new SubscriptionNotFoundException(subscriptionId);
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

        if (newSubscriptionType == null || this.getSubscriptionType(subscriptionId) == null) {
            return new StockCandleUpdateSubscriptionResponse(subscriptionId);
        } else {
            return new StockCandleUpdateSubscriptionResponse(subscriptionId, newSubscriptionType);
        }
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
                CandleData latest = response.getCandles().get(response.getCandles().size() - 1);
                subscription.updateLatestCandle(latest);
                // Align lastUpdateTime with the latest candle timestamp to avoid skipping boundary candles
                subscription.setLastUpdateTime(latest.getTimestamp());
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
        System.out.println("!!!!!!Checking for updates and send heartbeats");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        System.out.println("Subscriptions length:" + activeSubscriptions.size());

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

                System.out.println("#################################");
                System.out.println(subscription.getPlatformName());
                System.out.println(subscription.getStockSymbol());
                System.out.println(subscription.getTimeframe());
                System.out.println(subscription.getStartDate());
                System.out.println(subscription.getEndDate());
                System.out.println(subscription.getLastUpdateTime());
                System.out.println("#################################");
                System.out.println("new candles" + newCandles);

                if (!newCandles.isEmpty()) {
                    updatedCandles.addAll(newCandles);
                    System.out.println(updatedCandles.size() + " new candles");
                    hasUpdates = true;

                    // Update the latest candle info with the most recent candle
                    subscription.updateLatestCandle(newCandles.get(newCandles.size() - 1));
                }

                System.out.println("!!!!Has updates: " + hasUpdates);

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

                System.out.println("!!!!!!Checking for updates and send heartbeats before has updates check");

                // If there are updates, send them
                if (hasUpdates) {
                    System.out.println("!!!!!!If fork");
                    updateMessage.setUpdateType("UPDATE");
                    updateMessage.setUpdatedCandles(updatedCandles);
                    messagingTemplate.convertAndSend(subscription.getDestination(), updateMessage);
                    // Advance lastUpdateTime to the timestamp of the last candle we actually sent
                    LocalDateTime lastCandleTs = updatedCandles.get(updatedCandles.size() - 1).getTimestamp();
                    subscription.setLastUpdateTime(lastCandleTs);

                    updatedCandles.forEach(updatedCandle -> {
                        System.out.println("updatedCandle timestamp: " + updatedCandle.getTimestamp());
                        System.out.println("updatedCandle low: " + updatedCandle.getLowPrice());
                        System.out.println("updatedCandle high: " + updatedCandle.getHighPrice());
                        System.out.println("updatedCandle volume: " + updatedCandle.getVolume());
                        System.out.println("updatedCandle open: " + updatedCandle.getOpenPrice());
                        System.out.println("updatedCandle close: " + updatedCandle.getClosePrice());
                    });

                    loggingService.logAction("Sent " + updatedCandles.size() + " candle updates for subscription " +
                            subscription.getId() +
                            (subscription.getSubscriptionType() != null ?
                                    " (Type: " + subscription.getSubscriptionType() + ")" : ""));
                } else {
                    System.out.println("!!!!!!Else fork");
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
