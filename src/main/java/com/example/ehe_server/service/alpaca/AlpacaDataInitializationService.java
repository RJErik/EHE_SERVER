package com.example.ehe_server.service.alpaca;

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
public class AlpacaDataInitializationService {

    private static final String PLATFORM_NAME = "Alpaca";

    private final AlpacaCandleService candleService;
    private final AlpacaWebSocketClient webSocketClient;
    private final PlatformStockRepository stockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final MarketHoursService marketHoursService;

    private final Set<String> liveSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> historicalSyncInProgress = new ConcurrentHashMap<>();

    public AlpacaDataInitializationService(
            AlpacaCandleService candleService,
            AlpacaWebSocketClient webSocketClient,
            PlatformStockRepository stockRepository,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            MarketHoursService marketHoursService) {
        this.candleService = candleService;
        this.webSocketClient = webSocketClient;
        this.stockRepository = stockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.marketHoursService = marketHoursService;
    }

    @PostConstruct
    public void initialize() {
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Alpaca data initialization service ready");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async("alpacaTaskExecutor")
    public void initializeDataAsync() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Starting async Alpaca data synchronization");

            List<PlatformStock> stocks = stockRepository.findByPlatformPlatformName(PLATFORM_NAME);

            if (!stocks.isEmpty()) {
                loggingService.logAction("Found " + stocks.size() + " Alpaca symbols. Starting initialization...");

                // Separate stocks and crypto for different handling
                List<PlatformStock> stockSymbols = new ArrayList<>();
                List<PlatformStock> cryptoSymbols = new ArrayList<>();

                for (PlatformStock stock : stocks) {
                    String stockName = stock.getStock().getStockSymbol();
                    if (isCryptoSymbol(stockName)) {
                        cryptoSymbols.add(stock);
                    } else {
                        stockSymbols.add(stock);
                    }
                }

                loggingService.logAction("Found " + stockSymbols.size() + " stock symbols and " +
                        cryptoSymbols.size() + " crypto symbols");

                // Initialize crypto symbols immediately (24/7 market)
                for (PlatformStock crypto : cryptoSymbols) {
                    setupSymbol(crypto.getStock().getStockSymbol());
                }

                // Initialize stock symbols only if market is open, otherwise schedule for next open
                if (!stockSymbols.isEmpty()) {
                    if (marketHoursService.isMarketOpen()) {
                        loggingService.logAction("Market is open, initializing stock symbols");
                        for (PlatformStock stock : stockSymbols) {
                            setupSymbol(stock.getStock().getStockSymbol());
                        }
                    } else {
                        loggingService.logAction("Market is closed. Stock data will sync on next market open");
                    }
                }
            } else {
                loggingService.logAction("No Alpaca symbols found in database");
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
            // Check if it's a stock and market is closed
            if (!isCryptoSymbol(symbol) && !marketHoursService.isMarketOpen()) {
                loggingService.logAction("Skipping " + symbol + " - market is closed");
                return;
            }

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
     * Runs at 12:05 AM EST (after market close)
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "America/New_York")
    public void performDailyMaintenance() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Running daily maintenance");

            List<PlatformStock> stocks = stockRepository.findByPlatformPlatformName(PLATFORM_NAME);

            for (PlatformStock stock : stocks) {
                String symbol = stock.getStock().getStockSymbol();

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
     * Market open initialization for stock symbols
     * Runs at 9:25 AM EST (5 minutes before market open)
     */
    @Scheduled(cron = "0 25 9 * * MON-FRI", zone = "America/New_York")
    public void marketOpenInitialization() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Running market open initialization");

            List<PlatformStock> stocks = stockRepository.findByPlatformPlatformName(PLATFORM_NAME);

            for (PlatformStock stock : stocks) {
                String symbol = stock.getStock().getStockSymbol();

                // Only initialize stock symbols (not crypto)
                if (!isCryptoSymbol(symbol) && !liveSymbols.contains(symbol)) {
                    loggingService.logAction("Initializing stock symbol for market open: " + symbol);
                    setupSymbol(symbol);
                }
            }
        } catch (Exception e) {
            loggingService.logError("Error during market open initialization: " + e.getMessage(), e);
        }
    }

    /**
     * Hourly verification that WebSocket is connected with correct symbols
     */
    @Scheduled(cron = "0 0 * * * *")
    public void verifyWebSocketConnections() {
        try {
            userContextService.setUser("SYSTEM", "SYSTEM");
            loggingService.logAction("Verifying WebSocket connection");

            if (liveSymbols.isEmpty()) {
                loggingService.logAction("No live symbols configured");
                return;
            }

            if (!webSocketClient.isConnected()) {
                loggingService.logAction("WebSocket disconnected! Reconnecting with " +
                        liveSymbols.size() + " symbols");
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
        List<PlatformStock> stocks = stockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                PLATFORM_NAME, symbol);
        return stocks.isEmpty() ? null : stocks.get(0);
    }

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}