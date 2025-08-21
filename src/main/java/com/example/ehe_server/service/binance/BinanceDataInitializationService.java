package com.example.ehe_server.service.binance;

import com.example.ehe_server.dto.StocksByPlatformResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BinanceDataInitializationService {
    private static final String PLATFORM_NAME = "Binance";

    private final BinanceCandleService candleService;
    private final BinanceWebSocketClient webSocketClient;
    private final PlatformStockRepository stockRepository;
    private final StockServiceInterface stockService;
    private final LoggingServiceInterface loggingService;

    // Track active websocket subscriptions
    private final Map<String, Boolean> activeSubscriptions = new HashMap<>();
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

    @PostConstruct
    public void initialize() {
        //SYSTEM SET HERE
        userContextService.setUser("SYSTEM", "SYSTEM");
        loggingService.logAction("Initializing Binance data synchronization");

        // Check if we need to do initialization as system user
        if (!stockRepository.existsByPlatformName(PLATFORM_NAME)) {
            //SYSTEM SET HERE
            loggingService.logAction("No Binance stocks found. Please add them through the admin interface first.");
            return;
        }

        // Get existing symbols through direct repository access (system level)
        List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);

        if (!stocks.isEmpty()) {
            //SYSTEM SET HERE
            loggingService.logAction("Found " + stocks.size() + " Binance symbols in database");

            // Initialize each stock
            for (PlatformStock stock : stocks) {
                setupSymbol(stock.getStockSymbol());
            }
        } else {
            //SYSTEM SET HERE
            loggingService.logAction("No Binance symbols found in database. Add some through the API first.");
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
            //SYSTEM SET HERE
            loggingService.logError("Error syncing all Binance stocks: " + e.getMessage(), e);
        }
    }

    public void setupSymbol(String symbol) {
        try {
            // Step 1: Sync historical data
            //SYSTEM SET HERE
            loggingService.logAction("Starting historical data sync for " + symbol);
            candleService.syncHistoricalData(symbol);

            // Step 2: Set up real-time updates if not already subscribed
            if (!activeSubscriptions.getOrDefault(symbol, false)) {
                //SYSTEM SET HERE
                loggingService.logAction("Setting up real-time updates for " + symbol);
                PlatformStock stock = getStock(symbol);

                if (stock != null) {
                    webSocketClient.subscribeToKlineStream(symbol, "1m",
                            candleData -> candleService.processRealtimeCandle(candleData, stock));

                    activeSubscriptions.put(symbol, true);
                }
            }
        } catch (Exception e) {
            //SYSTEM SET HERE
            loggingService.logError("Failed to initialize " + symbol + " data: " + e.getMessage(), e);
        }
    }

    // Daily maintenance to ensure data completeness
    @Scheduled(cron = "0 5 0 * * *") // 00:05 UTC daily
    public void performDailyMaintenance() {
        try {
            //SYSTEM SET HERE
            loggingService.logAction("Running daily maintenance to ensure data completeness");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);
            for (PlatformStock stock : stocks) {
                candleService.syncHistoricalData(stock.getStockSymbol());
            }
        } catch (Exception e) {
            //SYSTEM SET HERE
            loggingService.logError("Error during daily maintenance: " + e.getMessage(), e);
        }
    }

    // Verify WebSocket connections hourly
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void verifyWebSocketConnections() {
        try {
            //SYSTEM SET HERE
            loggingService.logAction("Verifying WebSocket connections");

            List<PlatformStock> stocks = stockRepository.findByPlatformName(PLATFORM_NAME);
            for (PlatformStock stock : stocks) {
                String symbol = stock.getStockSymbol();

                // Reset subscription flag to force reconnection if needed
                if (activeSubscriptions.getOrDefault(symbol, false)) {
                    activeSubscriptions.put(symbol, false);
                    setupSymbol(symbol);
                }
            }
        } catch (Exception e) {
            //SYSTEM SET HERE
            loggingService.logError("Error verifying WebSocket connections: " + e.getMessage(), e);
        }
    }

    private PlatformStock getStock(String symbol) {
        List<PlatformStock> stocks = stockRepository.findByPlatformNameAndStockSymbol(
                PLATFORM_NAME, symbol);

        return stocks.isEmpty() ? null : stocks.get(0);
    }
}
