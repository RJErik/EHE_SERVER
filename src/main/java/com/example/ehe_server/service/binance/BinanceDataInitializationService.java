package com.example.ehe_server.service.binance;

import com.example.ehe_server.dto.StocksByPlatformResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BinanceDataInitializationService {
    private static final String PLATFORM_NAME = "Binance";

    private final BinanceCandleService candleService;
    private final BinanceWebSocketClient webSocketClient;
    private final PlatformStockRepository stockRepository;
    private final StockServiceInterface stockService;
    private final LoggingServiceInterface loggingService;

    // Track active websocket subscriptions
    private final Map<String, Boolean> activeSubscriptions = new HashMap<>();
    private final Map<String, Boolean> historicalSyncInProgress = new ConcurrentHashMap<>(); // NEW
    private final UserContextService userContextService;

    public BinanceDataInitializationService(
            BinanceCandleService candleService,
            BinanceWebSocketClient webSocketClient,
            PlatformStockRepository stockRepository,
            StockServiceInterface stockService,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.candleService = candleService;
        this.webSocketClient = webSocketClient;
        this.stockRepository = stockRepository;
        this.stockService = stockService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    // This now only does basic setup, doesn't block startup
    @PostConstruct
    public void initialize() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Binance data initialization service ready");
    }

    // This runs AFTER the application is fully started
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeDataAsync() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Starting async Binance data synchronization");

            // Check if we need to do initialization as system user
            if (!stockRepository.existsByPlatformName(PLATFORM_NAME)) {
                loggingService.logAction("No Binance stocks found. Please add them through the admin interface first.");
                return;
            }

            // Get existing symbols through direct repository access (system level)
            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);

            if (!stocks.isEmpty()) {
                loggingService.logAction("Found " + stocks.size() + " Binance symbols in database");

                // Initialize each stock asynchronously
                for (PlatformStock stock : stocks) {
                    setupSymbol(stock.getStockSymbol());
                }
            } else {
                loggingService.logAction("No Binance symbols found in database. Add some through the API first.");
            }
        } catch (Exception e) {
            loggingService.logError("Error during async initialization: " + e.getMessage(), e);
        }
    }

    // We'll need to run this with elevated privileges or mock the user context
    @Transactional
    public void syncAllBinanceStocks() {
        try {
            // Note: stockService.getStocksByPlatform requires a user context
            // You may need to handle this differently in your actual implementation
            StocksByPlatformResponse stocksResponse = stockService.getStocksByPlatform(PLATFORM_NAME);

            List<String> stocks = stocksResponse.getStocks();

            if (stocks != null && !stocks.isEmpty()) {
                for (String symbol : stocks) {
                    setupSymbol(symbol);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error syncing all Binance stocks: " + e.getMessage(), e);
        }
    }

    public void setupSymbol(String symbol) {
        try {
            // Mark as syncing
            historicalSyncInProgress.put(symbol, true);
            activeSubscriptions.put(symbol, false); // Not live yet

            loggingService.logAction("Starting historical data sync for " + symbol);
            candleService.syncHistoricalData(symbol);

            // Historical sync completed, now go live
            historicalSyncInProgress.put(symbol, false);

            // Set up real-time updates
            loggingService.logAction("Setting up real-time updates for " + symbol);
            PlatformStock stock = getStock(symbol);
            if (stock != null) {
                webSocketClient.subscribeToKlineStream(symbol, "1m",
                        candleData -> candleService.processRealtimeCandle(candleData, stock));
                activeSubscriptions.put(symbol, true); // Now live
            }
        } catch (Exception e) {
            // Cleanup on error
            historicalSyncInProgress.put(symbol, false);
            activeSubscriptions.put(symbol, false);
            loggingService.logError("Failed to initialize " + symbol + " data: " + e.getMessage(), e);
        }
    }

    // Daily maintenance to ensure data completeness
    @Scheduled(cron = "0 5 0 * * *") // 00:05 UTC daily
    public void performDailyMaintenance() {
        try {
            loggingService.logAction("Running daily maintenance to ensure data completeness");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);
            for (PlatformStock stock : stocks) {
                String symbol = stock.getStockSymbol();

                // ONLY verify if we're in live mode (not syncing historical data)
                boolean isSyncing = historicalSyncInProgress.getOrDefault(symbol, false);
                boolean isLive = activeSubscriptions.getOrDefault(symbol, false);

                if (isSyncing || isLive) {
                    loggingService.logAction("Skipping historical sync for " + symbol +  " sync already in progress or the websocket is live.");
                    continue;
                }
                candleService.syncHistoricalData(stock.getStockSymbol());
            }
        } catch (Exception e) {
            loggingService.logError("Error during daily maintenance: " + e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void verifyWebSocketConnections() {
        try {
            loggingService.logAction("Verifying WebSocket connections");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);
            for (PlatformStock stock : stocks) {
                String symbol = stock.getStockSymbol();

                // ONLY verify if we're in live mode (not syncing historical data)
                boolean isSyncing = historicalSyncInProgress.getOrDefault(symbol, false);
                boolean isLive = activeSubscriptions.getOrDefault(symbol, false);

                if (isSyncing) {
                    loggingService.logAction("Skipping WebSocket verification for " + symbol + " - historical sync in progress");
                    continue;
                }

                if (isLive) {
                    // This symbol should have an active WebSocket - verify it
                    boolean disconnected = webSocketClient.disconnectSymbol(symbol);
                    if (disconnected) {
                        loggingService.logAction("Disconnected existing WebSocket for " + symbol);
                    }

                    activeSubscriptions.put(symbol, false);

                    // Small delay then reconnect WebSocket only
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // Reconnect WebSocket only (no historical sync)
                    reconnectWebSocketOnly(symbol);
                } else {
                    loggingService.logAction("Symbol " + symbol + " not in live mode, skipping verification");
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error verifying WebSocket connections: " + e.getMessage(), e);
        }
    }

    private void reconnectWebSocketOnly(String symbol) {
        try {
            PlatformStock stock = getStock(symbol);
            if (stock != null) {
                webSocketClient.subscribeToKlineStream(symbol, "1m",
                        candleData -> candleService.processRealtimeCandle(candleData, stock));
                activeSubscriptions.put(symbol, true);
                loggingService.logAction("Reconnected WebSocket for " + symbol);
            }
        } catch (Exception e) {
            loggingService.logError("Failed to reconnect WebSocket for " + symbol + ": " + e.getMessage(), e);
        }
    }

    private PlatformStock getStock(String symbol) {
        List<PlatformStock> stocks = stockRepository.findByPlatformNameAndStockSymbol(
                PLATFORM_NAME, symbol);

        return stocks.isEmpty() ? null : stocks.get(0);
    }
}