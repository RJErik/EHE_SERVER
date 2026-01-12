package ehe_server.service.binance;

import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.binance.BinanceApiClientInterface;
import ehe_server.service.intf.binance.BinanceCandleServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BinanceCandleService implements BinanceCandleServiceInterface {
    private static final String PLATFORM_NAME = "Binance";
    private static final int MAX_CANDLES_PER_REQUEST = 1000;
    private static final int WEEK_IN_MINUTES = 7 * 24 * 60;
    private static final int HALF_WEEK_IN_MINUTES = WEEK_IN_MINUTES / 2;

    private final BinanceApiClientInterface binanceApiClient;
    private final MarketCandleRepository marketCandleRepository;
    private final PlatformStockRepository platformStockRepository;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextServiceInterface userContextService;

    public BinanceCandleService(
            BinanceApiClientInterface binanceApiClient,
            MarketCandleRepository marketCandleRepository,
            PlatformStockRepository platformStockRepository,
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextServiceInterface userContextService) {
        this.binanceApiClient = binanceApiClient;
        this.marketCandleRepository = marketCandleRepository;
        this.platformStockRepository = platformStockRepository;
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
        PlatformStock stock = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(PLATFORM_NAME, symbol).getFirst();

        MarketCandle latestCandle = marketCandleRepository
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
    @Override
    public void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        // Step 1: Deduplicate within the batch (group by timeframe + timestamp)
        // Key: "TIMEFRAME|TIMESTAMP" -> keeps last occurrence
        Map<String, MarketCandle> uniqueCandles = new LinkedHashMap<>();
        for (MarketCandle candle : candles) {
            String key = candle.getTimeframe() + "|" + candle.getTimestamp();
            uniqueCandles.put(key, candle);
        }

        // Step 2: Group by timeframe for efficient batch queries
        Map<MarketCandle.Timeframe, List<MarketCandle>> byTimeframe = uniqueCandles.values().stream()
                .collect(Collectors.groupingBy(MarketCandle::getTimeframe));

        // Step 3: Batch query existing candles for each timeframe
        Map<String, MarketCandle> existingMap = new HashMap<>();

        for (Map.Entry<MarketCandle.Timeframe, List<MarketCandle>> entry : byTimeframe.entrySet()) {
            MarketCandle.Timeframe timeframe = entry.getKey();
            List<LocalDateTime> timestamps = entry.getValue().stream()
                    .map(MarketCandle::getTimestamp)
                    .collect(Collectors.toList());

            List<MarketCandle> existingCandles = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampIn(stock, timeframe, timestamps);

            for (MarketCandle existing : existingCandles) {
                String key = existing.getTimeframe() + "|" + existing.getTimestamp();
                existingMap.put(key, existing);
            }
        }

        // Step 4: Process - update existing or keep new
        List<MarketCandle> candlesToSave = new ArrayList<>();
        List<MarketCandle> savedCandles = new ArrayList<>();  // For aggregation tracking

        for (MarketCandle candle : uniqueCandles.values()) {
            String key = candle.getTimeframe() + "|" + candle.getTimestamp();
            MarketCandle existing = existingMap.get(key);

            if (existing != null) {
                // Update existing candle
                existing.setOpenPrice(candle.getOpenPrice());
                existing.setHighPrice(candle.getHighPrice());
                existing.setLowPrice(candle.getLowPrice());
                existing.setClosePrice(candle.getClosePrice());
                existing.setVolume(candle.getVolume());
                candlesToSave.add(existing);
                savedCandles.add(existing);
            } else {
                // New candle
                candlesToSave.add(candle);
                savedCandles.add(candle);
            }
        }

        // Step 5: Save all candles in one batch
        marketCandleRepository.saveAll(candlesToSave);

        // Step 6: Aggregate (only M1 candles need aggregation)
        List<MarketCandle> minuteCandles = savedCandles.stream()
                .filter(c -> c.getTimeframe() == MarketCandle.Timeframe.M1)
                .collect(Collectors.toList());

        if (!minuteCandles.isEmpty()) {
            aggregateCandles(stock, minuteCandles);
        }
    }

    /**
     * Fetches candles in batches from start time to present.
     * Continuously updates target end time to capture real-time data.
     */
    private void fetchCandlesInRange(PlatformStock stock, String symbol, Instant startInstant) {
        long currentStartTime = startInstant.toEpochMilli();
        long endTime = Instant.now().toEpochMilli();

        int totalCandlesFetched = 0;

        while (currentStartTime < endTime) {
            try {
                // Always update target end time to capture latest data
                endTime = Instant.now().toEpochMilli();

                ResponseEntity<String> response = binanceApiClient.getKlines(
                        symbol, "1m", currentStartTime, null, MAX_CANDLES_PER_REQUEST);
                List<MarketCandle> candles = parseCandles(response.getBody(), stock);

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
                    endTime = Instant.now().toEpochMilli();
                    if (currentStartTime >= endTime) {
                        break;
                    }
                    // Continue to fetch any remaining data
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
            ResponseEntity<String> response = binanceApiClient.getKlines(
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
                groupedCandles.computeIfAbsent(timeframeStart, _ -> new ArrayList<>()).add(candle);
            }

            List<MarketCandle> candlesToSave = new ArrayList<>();

            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                LocalDateTime timeframeEnd = timeframeStart.plusMinutes(minutes);

                // === FIX: Fetch ALL M1 candles in this timeframe period ===
                List<MarketCandle> allCandlesInPeriod = marketCandleRepository
                        .findByPlatformStockAndTimeframeAndTimestampBetween(
                                stock,
                                MarketCandle.Timeframe.M1,
                                timeframeStart,
                                timeframeEnd.minusSeconds(1)
                        );

                if (allCandlesInPeriod.isEmpty()) {
                    continue;
                }

                // Sort by timestamp
                allCandlesInPeriod.sort(Comparator.comparing(MarketCandle::getTimestamp));

                // Calculate ALL values from source candles
                BigDecimal openPrice = allCandlesInPeriod.getFirst().getOpenPrice();
                BigDecimal closePrice = allCandlesInPeriod.getLast().getClosePrice();

                BigDecimal highPrice = allCandlesInPeriod.stream()
                        .map(MarketCandle::getHighPrice)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal lowPrice = allCandlesInPeriod.stream()
                        .map(MarketCandle::getLowPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal volume = allCandlesInPeriod.stream()
                        .map(MarketCandle::getVolume)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Find or create the aggregated candle
                MarketCandle aggregatedCandle = marketCandleRepository
                        .findByPlatformStockAndTimeframeAndTimestampEquals(
                                stock, timeframe, timeframeStart)
                        .orElseGet(() -> {
                            MarketCandle newCandle = new MarketCandle();
                            newCandle.setPlatformStock(stock);
                            newCandle.setTimeframe(timeframe);
                            newCandle.setTimestamp(timeframeStart);
                            return newCandle;
                        });

                // Update all fields
                aggregatedCandle.setOpenPrice(openPrice);
                aggregatedCandle.setHighPrice(highPrice);
                aggregatedCandle.setLowPrice(lowPrice);
                aggregatedCandle.setClosePrice(closePrice);
                aggregatedCandle.setVolume(volume);

                candlesToSave.add(aggregatedCandle);
            }

            if (!candlesToSave.isEmpty()) {
                marketCandleRepository.saveAll(candlesToSave);
            }

        } catch (Exception e) {
            loggingService.logError(String.format("Error aggregating to %s: %s",
                    timeframe, e.getMessage()), e);
        }
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

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(openTime), ZoneOffset.UTC);

            MarketCandle candle = marketCandleRepository
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

            candle = marketCandleRepository.save(candle);

            aggregateCandles(stock, Collections.singletonList(candle));
        } catch (Exception e) {
            loggingService.logError("Error processing realtime candle: " + e.getMessage(), e);
            throw e;
        }
    }
}