package com.example.ehe_server.service.binance;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.Platform;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Stock;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.PlatformRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.StockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.binance.BinanceApiClientInterface;
import com.example.ehe_server.service.intf.binance.BinanceCandleServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BinanceCandleService implements BinanceCandleServiceInterface {
    private static final String PLATFORM_NAME = "Binance";
    private static final int MAX_CANDLES_PER_REQUEST = 1000;
    private static final int WEEK_IN_MINUTES = 7 * 24 * 60;
    private static final int HALF_WEEK_IN_MINUTES = WEEK_IN_MINUTES / 2;
    private static final int UPDATE_TARGET_TIME_EVERY_N_BATCHES = 5;
    private static final int UPDATE_TARGET_TIME_AFTER_MINUTES = 15;

    private final BinanceApiClientInterface apiClient;
    private final MarketCandleRepository candleRepository;
    private final PlatformStockRepository stockRepository;
    private final PlatformRepository platformRepository;
    private final StockRepository stockRepositoryForStock;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public BinanceCandleService(
            BinanceApiClientInterface apiClient,
            MarketCandleRepository candleRepository,
            PlatformStockRepository stockRepository,
            PlatformRepository platformRepository,
            StockRepository stockRepositoryForStock,
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextService userContextService) {
        this.apiClient = apiClient;
        this.candleRepository = candleRepository;
        this.stockRepository = stockRepository;
        this.platformRepository = platformRepository;
        this.stockRepositoryForStock = stockRepositoryForStock;
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    /**
     * Synchronizes historical market data for a given symbol.
     * If existing data is found, updates from the latest candle to present.
     * If no data exists, searches backward to find earliest available data point.
     */
    public void syncHistoricalData(String symbol) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        PlatformStock stock = getOrCreateStock(symbol);

        MarketCandle latestCandle = candleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

        if (latestCandle != null) {
            loggingService.logAction(String.format("Updating %s from %s to present",
                    symbol, latestCandle.getTimestamp()));

            Instant startInstant = latestCandle.getTimestamp().toInstant(ZoneOffset.UTC);
            fetchCandlesInRange(stock, symbol, startInstant);
        } else {
            loggingService.logAction(String.format("No existing data for %s, searching for earliest available data", symbol));

            Instant firstCheckpoint = Instant.now();
            Optional<Instant> earliestDataPoint = findEarliestAvailableData(symbol, firstCheckpoint);

            if (earliestDataPoint.isPresent()) {
                loggingService.logAction(String.format("Fetching %s from %s to present",
                        symbol, earliestDataPoint.get().atZone(ZoneOffset.UTC)));
                fetchCandlesInRange(stock, symbol, earliestDataPoint.get());
            } else {
                loggingService.logAction(String.format("No historical data available for %s", symbol));
            }
        }
    }

    /**
     * Saves a batch of candles and their aggregations in a single transaction.
     * Updates existing candles or creates new ones as needed.
     */
    @Transactional
    public void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        List<MarketCandle> candlesToSave = new ArrayList<>();

        for (MarketCandle candle : candles) {
            Optional<MarketCandle> existingCandle = candleRepository
                    .findByPlatformStockAndTimeframeAndTimestampEquals(
                            stock, candle.getTimeframe(), candle.getTimestamp());

            if (existingCandle.isPresent()) {
                MarketCandle existing = existingCandle.get();
                existing.setOpenPrice(candle.getOpenPrice());
                existing.setHighPrice(candle.getHighPrice());
                existing.setLowPrice(candle.getLowPrice());
                existing.setClosePrice(candle.getClosePrice());
                existing.setVolume(candle.getVolume());
                candlesToSave.add(existing);
            } else {
                candlesToSave.add(candle);
            }
        }

        candleRepository.saveAll(candlesToSave);
        aggregateCandles(stock, candlesToSave);
    }

    /**
     * Fetches candles in batches from start time to present.
     * Dynamically updates target end time to capture real-time data.
     */
    private void fetchCandlesInRange(PlatformStock stock, String symbol, Instant startInstant) {
        Instant targetEndTime = Instant.now();
        long currentStartTime = startInstant.toEpochMilli();
        long endTime = targetEndTime.toEpochMilli();

        int totalCandlesFetched = 0;
        int batchesFetched = 0;
        Instant lastTimeUpdate = Instant.now();

        while (currentStartTime < endTime) {
            try {
                // Periodically update target end time to capture latest data
                if (batchesFetched % UPDATE_TARGET_TIME_EVERY_N_BATCHES == 0 ||
                        Duration.between(lastTimeUpdate, Instant.now()).toMinutes() >= UPDATE_TARGET_TIME_AFTER_MINUTES) {
                    targetEndTime = Instant.now();
                    endTime = targetEndTime.toEpochMilli();
                    lastTimeUpdate = Instant.now();
                }

                ResponseEntity<String> response = apiClient.getKlines(
                        symbol, "1m", currentStartTime, null, MAX_CANDLES_PER_REQUEST);
                List<MarketCandle> candles = parseCandles(response.getBody(), stock);
                batchesFetched++;

                if (candles.isEmpty()) {
                    break;
                }

                saveCandleBatch(stock, candles);
                totalCandlesFetched += candles.size();

                if (totalCandlesFetched % 5000 == 0) {
                    loggingService.logAction(String.format("Progress: %d candles fetched for %s",
                            totalCandlesFetched, symbol));
                }

                MarketCandle lastCandle = candles.getLast();
                currentStartTime = lastCandle.getTimestamp()
                        .plusMinutes(1)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();

                // Check if we received a partial batch (likely end of available data)
                if (candles.size() < MAX_CANDLES_PER_REQUEST) {
                    targetEndTime = Instant.now();
                    if (currentStartTime < targetEndTime.toEpochMilli()) {
                        endTime = targetEndTime.toEpochMilli();
                        continue;
                    }
                    break;
                }
            } catch (Exception e) {
                loggingService.logError("Error fetching candles: " + e.getMessage(), e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        loggingService.logAction(String.format("Completed %s sync: %d candles fetched",
                symbol, totalCandlesFetched));
    }

    /**
     * Searches backward week-by-week to find the earliest available data point.
     * Uses an expanding search when gaps are detected.
     */
    private Optional<Instant> findEarliestAvailableData(String symbol, Instant firstCheckpoint) {
        Instant checkPoint = firstCheckpoint;
        Instant lastDataPoint = null;
        boolean foundGap = false;

        while (!foundGap) {
            checkPoint = checkPoint.minus(Duration.ofMinutes(WEEK_IN_MINUTES));

            boolean hasCandles = checkForCandles(symbol, checkPoint,
                    checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES)));

            if (hasCandles) {
                lastDataPoint = checkPoint;
            } else {
                // Expand search range to confirm gap
                Instant expandedStart = checkPoint.minus(Duration.ofMinutes(HALF_WEEK_IN_MINUTES));
                Instant expandedEnd = checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES + HALF_WEEK_IN_MINUTES));

                boolean hasExpandedCandles = checkForCandles(symbol, expandedStart, expandedEnd);

                if (hasExpandedCandles) {
                    lastDataPoint = expandedStart;
                } else {
                    foundGap = true;
                }
            }
        }

        return Optional.ofNullable(lastDataPoint);
    }

    /**
     * Checks if any candles exist in the specified time range.
     */
    private boolean checkForCandles(String symbol, Instant startTime, Instant endTime) {
        try {
            ResponseEntity<String> response = apiClient.getKlines(
                    symbol, "1m",
                    startTime.toEpochMilli(),
                    endTime.toEpochMilli(),
                    1);

            JsonNode candlesArray = objectMapper.readTree(response.getBody());
            return !candlesArray.isEmpty();
        } catch (Exception e) {
            loggingService.logError("Error checking for candles: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parses Binance API candle data into MarketCandle entities.
     */
    private List<MarketCandle> parseCandles(String responseBody, PlatformStock stock) {
        List<MarketCandle> candles = new ArrayList<>();
        try {
            JsonNode candlesArray = objectMapper.readTree(responseBody);
            for (JsonNode candleNode : candlesArray) {
                long openTime = candleNode.get(0).asLong();
                String open = candleNode.get(1).asText();
                String high = candleNode.get(2).asText();
                String low = candleNode.get(3).asText();
                String close = candleNode.get(4).asText();
                String volume = candleNode.get(5).asText();

                MarketCandle candle = new MarketCandle();
                candle.setPlatformStock(stock);
                candle.setTimeframe(MarketCandle.Timeframe.M1);
                candle.setTimestamp(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(openTime), ZoneOffset.UTC));
                candle.setOpenPrice(new BigDecimal(open));
                candle.setHighPrice(new BigDecimal(high));
                candle.setLowPrice(new BigDecimal(low));
                candle.setClosePrice(new BigDecimal(close));
                candle.setVolume(new BigDecimal(volume));

                candles.add(candle);
            }
        } catch (Exception e) {
            loggingService.logError("Error parsing candle data: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse candle data", e);
        }

        return candles;
    }

    /**
     * Aggregates minute candles into higher timeframes (5m, 15m, 1h, 4h, 1d).
     */
    private void aggregateCandles(PlatformStock stock, List<MarketCandle> minuteCandles) {
        if (minuteCandles.isEmpty()) return;

        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M5, 5);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M15, 15);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H1, 60);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H4, 240);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.D1, 1440);
    }

    /**
     * Aggregates minute candles into a specific timeframe.
     */
    private void aggregateTimeframe(PlatformStock stock, List<MarketCandle> minuteCandles,
                                    MarketCandle.Timeframe timeframe, int minutes) {
        try {
            Map<LocalDateTime, List<MarketCandle>> groupedCandles = new HashMap<>();

            for (MarketCandle candle : minuteCandles) {
                LocalDateTime timeframeStart = calculateTimeframeStart(candle.getTimestamp(), minutes);
                groupedCandles.computeIfAbsent(timeframeStart, k -> new ArrayList<>()).add(candle);
            }

            List<MarketCandle> candles = new ArrayList<>();
            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                List<MarketCandle> candlesInPeriod = entry.getValue();

                if (!candlesInPeriod.isEmpty()) {
                    MarketCandle candle = candleRepository
                            .findByPlatformStockAndTimeframeAndTimestampEquals(
                                    stock, timeframe, timeframeStart)
                            .orElse(null);

                    if (candle == null) {
                        candle = createAggregatedCandle(stock, timeframe, timeframeStart, candlesInPeriod);
                    } else {
                        updateExistingCandle(candle, candlesInPeriod);
                    }

                    candles.add(candle);
                }
            }

            if (!candles.isEmpty()) {
                candleRepository.saveAll(candles);
            }
        } catch (Exception e) {
            loggingService.logError(String.format("Error aggregating to %s: %s",
                    timeframe, e.getMessage()), e);
        }
    }

    /**
     * Updates an existing aggregated candle with new minute candle data.
     */
    private void updateExistingCandle(MarketCandle existingCandle, List<MarketCandle> minuteCandles) {
        minuteCandles.sort(Comparator.comparing(MarketCandle::getTimestamp));

        BigDecimal newHigh = minuteCandles.stream()
                .map(MarketCandle::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (newHigh.compareTo(existingCandle.getHighPrice()) > 0) {
            existingCandle.setHighPrice(newHigh);
        }

        BigDecimal newLow = minuteCandles.stream()
                .map(MarketCandle::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (newLow.compareTo(existingCandle.getLowPrice()) < 0) {
            existingCandle.setLowPrice(newLow);
        }

        existingCandle.setClosePrice(minuteCandles.getLast().getClosePrice());
    }

    /**
     * Calculates the start timestamp for a given timeframe.
     */
    private LocalDateTime calculateTimeframeStart(LocalDateTime time, int minutes) {
        int minuteOfDay = time.getHour() * 60 + time.getMinute();
        int timeframeIndex = minuteOfDay / minutes;

        return time
                .withHour((timeframeIndex * minutes) / 60)
                .withMinute((timeframeIndex * minutes) % 60)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Creates a new aggregated candle from multiple minute candles.
     */
    private MarketCandle createAggregatedCandle(PlatformStock stock, MarketCandle.Timeframe timeframe,
                                                LocalDateTime timeframeStart, List<MarketCandle> candles) {
        BigDecimal open = candles.getFirst().getOpenPrice();
        BigDecimal close = candles.getLast().getClosePrice();
        BigDecimal high = candles.stream()
                .map(MarketCandle::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal low = candles.stream()
                .map(MarketCandle::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = candles.stream()
                .map(MarketCandle::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MarketCandle aggregated = new MarketCandle();
        aggregated.setPlatformStock(stock);
        aggregated.setTimeframe(timeframe);
        aggregated.setTimestamp(timeframeStart);
        aggregated.setOpenPrice(open);
        aggregated.setHighPrice(high);
        aggregated.setLowPrice(low);
        aggregated.setClosePrice(close);
        aggregated.setVolume(volume);

        return aggregated;
    }

    /**
     * Processes real-time candle updates from WebSocket.
     */
    @Transactional
    public void processRealtimeCandle(JsonNode candleData, PlatformStock stock) {
        if (!userContextService.isAuthenticated()) {
            userContextService.setUser("SYSTEM", "SYSTEM");
        }

        try {
            JsonNode k = candleData.get("k");
            long openTime = k.get("t").asLong();
            boolean isFinal = k.get("x").asBoolean();

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(openTime), ZoneOffset.UTC);

            MarketCandle candle = candleRepository
                    .findByPlatformStockAndTimeframeAndTimestampEquals(
                            stock, MarketCandle.Timeframe.M1, timestamp)
                    .orElseGet(() -> {
                        MarketCandle newCandle = new MarketCandle();
                        newCandle.setPlatformStock(stock);
                        newCandle.setTimeframe(MarketCandle.Timeframe.M1);
                        newCandle.setTimestamp(timestamp);
                        return newCandle;
                    });

            candle.setOpenPrice(new BigDecimal(k.get("o").asText()));
            candle.setHighPrice(new BigDecimal(k.get("h").asText()));
            candle.setLowPrice(new BigDecimal(k.get("l").asText()));
            candle.setClosePrice(new BigDecimal(k.get("c").asText()));
            candle.setVolume(new BigDecimal(k.get("v").asText()));

            candle = candleRepository.save(candle);

            if (isFinal) {
                aggregateCandles(stock, Collections.singletonList(candle));
            }
        } catch (Exception e) {
            loggingService.logError("Error processing realtime candle: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves or creates a PlatformStock entity for the given symbol.
     */
    private PlatformStock getOrCreateStock(String symbol) {
        List<PlatformStock> stocks = stockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                PLATFORM_NAME, symbol);
        if (!stocks.isEmpty()) {
            return stocks.getFirst();
        }

        Platform platform = platformRepository.findByPlatformName(PLATFORM_NAME)
                .orElseThrow(() -> new RuntimeException("Platform not found: " + PLATFORM_NAME));

        Stock stock = stockRepositoryForStock.findByStockSymbol(symbol)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setStockSymbol(symbol);
                    return stockRepositoryForStock.save(newStock);
                });

        PlatformStock platformStock = new PlatformStock();
        platformStock.setPlatform(platform);
        platformStock.setStock(stock);
        return stockRepository.save(platformStock);
    }
}