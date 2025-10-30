package com.example.ehe_server.service.binance;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BinanceDataInitializationService {
    private static final String PLATFORM_NAME = "Binance";

    private final BinanceCandleService candleService;
    private final BinanceWebSocketClient webSocketClient;
    private final PlatformStockRepository stockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    // Track which symbols are live and synced
    private final Set<String> liveSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> historicalSyncInProgress = new ConcurrentHashMap<>();

    public BinanceDataInitializationService(
            BinanceCandleService candleService,
            BinanceWebSocketClient webSocketClient,
            PlatformStockRepository stockRepository,
            LoggingServiceInterface loggingService,
            UserContextService userContextService) {
        this.candleService = candleService;
        this.webSocketClient = webSocketClient;
        this.stockRepository = stockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @PostConstruct
    public void initialize() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance data initialization service ready");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeDataAsync() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Starting async Binance data synchronization");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);

            if (!stocks.isEmpty()) {
                loggingService.logAction("Found " + stocks.size() + " Binance symbols. Starting initialization...");

                // Initialize each symbol
                for (PlatformStock stock : stocks) {
                    setupSymbol(stock.getStockSymbol());
                }
            } else {
                loggingService.logAction("No Binance symbols found in database");
            }
        } catch (Exception e) {
            loggingService.logError("Error during async initialization: " + e.getMessage(), e);
        }
    }

    /**
     * Sets up a symbol: sync historical data, then add to live WebSocket
     */
    public void setupSymbol(String symbol) {
        try {
            historicalSyncInProgress.put(symbol, true);

            loggingService.logAction("Starting historical data sync for " + symbol);
            candleService.syncHistoricalData(symbol);

            // Get the stock entity for handler
            PlatformStock stock = getStock(symbol);
            if (stock == null) {
                throw new RuntimeException("Stock not found: " + symbol);
            }

            // Register handler with WebSocket
            webSocketClient.registerHandler(symbol,
                    candleData -> candleService.processRealtimeCandle(candleData, stock));

            // Add to live symbols (this triggers WebSocket reconnection)
            liveSymbols.add(symbol);
            webSocketClient.updateSubscriptions(new ArrayList<>(liveSymbols));

            historicalSyncInProgress.put(symbol, false);
            loggingService.logAction("Successfully initialized " + symbol);

        } catch (Exception e) {
            historicalSyncInProgress.put(symbol, false);
            loggingService.logError("Failed to initialize " + symbol + ": " + e.getMessage(), e);
        }
    }

    /**
     * Remove a symbol from live subscriptions
     */
    public void removeSymbol(String symbol) {
        try {
            liveSymbols.remove(symbol);
            webSocketClient.unregisterHandler(symbol);
            webSocketClient.updateSubscriptions(new ArrayList<>(liveSymbols));

            loggingService.logAction("Removed " + symbol + " from live subscriptions. " +
                    "Remaining symbols: " + liveSymbols.size());
        } catch (Exception e) {
            loggingService.logError("Failed to remove symbol " + symbol + ": " + e.getMessage(), e);
        }
    }

    /**
     * Daily maintenance to sync any gaps in historical data
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void performDailyMaintenance() {
        try {
            loggingService.logAction("Running daily maintenance");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);
            for (PlatformStock stock : stocks) {
                String symbol = stock.getStockSymbol();

                // Skip if syncing or not live
                if (historicalSyncInProgress.getOrDefault(symbol, false)) {
                    loggingService.logAction("Skipping " + symbol + " - historical sync in progress");
                    continue;
                }

                if (liveSymbols.contains(symbol)) {
                    loggingService.logAction("Syncing any missing data for " + symbol);
                    candleService.syncHistoricalData(symbol);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error during daily maintenance: " + e.getMessage(), e);
        }
    }

    /**
     * Hourly verification that WebSocket is connected with correct symbols
     */
    @Scheduled(cron = "0 0 * * * *")
    public void verifyWebSocketConnections() {
        try {
            loggingService.logAction("Verifying WebSocket connection");

            if (liveSymbols.isEmpty()) {
                loggingService.logAction("No live symbols configured");
                return;
            }

            if (!webSocketClient.isConnected()) {
                loggingService.logAction("WebSocket disconnected! Reconnecting with " + liveSymbols.size() + " symbols");
                webSocketClient.updateSubscriptions(new ArrayList<>(liveSymbols));
            } else {
                // Verify subscription count matches
                int expected = liveSymbols.size();
                int actual = webSocketClient.getSubscriptionCount();

                if (expected != actual) {
                    loggingService.logAction("Subscription mismatch! Expected: " + expected +
                            ", Actual: " + actual + ". Reconnecting...");
                    webSocketClient.updateSubscriptions(new ArrayList<>(liveSymbols));
                } else {
                    loggingService.logAction("WebSocket healthy with " + actual + " subscriptions");
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error verifying WebSocket: " + e.getMessage(), e);
        }
    }

    /**
     * Get list of symbols currently live
     */
    public Set<String> getLiveSymbols() {
        return new HashSet<>(liveSymbols);
    }

    /**
     * Get sync status for a symbol
     */
    public boolean isSyncing(String symbol) {
        return historicalSyncInProgress.getOrDefault(symbol, false);
    }

    private PlatformStock getStock(String symbol) {
        List<PlatformStock> stocks = stockRepository.findByPlatformNameAndStockSymbol(
                PLATFORM_NAME, symbol);
        return stocks.isEmpty() ? null : stocks.get(0);
    }
}