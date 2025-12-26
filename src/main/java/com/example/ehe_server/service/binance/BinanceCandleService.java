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

    // How often to update the target end time during fetching
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

    // @Transactional REMOVED FROM HERE
    public void syncHistoricalData(String symbol) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        PlatformStock stock = getOrCreateStock(symbol);

        // Find latest existing candle
        MarketCandle latestCandle = candleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);
        if (latestCandle != null) {
            // We have existing data, start from the latest candle
            //SYSTEM SET HERE
            loggingService.logAction("Found existing candles for " + symbol + ". Latest candle at " + latestCandle.getTimestamp());
            // Start from the latest candle
            Instant startInstant = latestCandle.getTimestamp()
                    .toInstant(ZoneOffset.UTC);
            //SYSTEM SET HERE
            loggingService.logAction("Updating candles for " + symbol + " from " + startInstant.atZone(ZoneOffset.UTC) +
                    " to present (with periodic target time updates)");
            // Fetch with dynamic end time updates (null means use current time with updates)
            fetchCandlesInRange(stock, symbol, startInstant, null);
        } else {
            // No existing data, need to find how far back data is available
            //SYSTEM SET HERE
            loggingService.logAction("No existing candles for " + symbol + ". Finding earliest available data...");
            // Find the earliest available data using the backward search strategy
            Instant firstCheckpoint = Instant.now();
            Optional<Instant> earliestDataPoint = findEarliestAvailableData(symbol, firstCheckpoint);

            if (earliestDataPoint.isPresent()) {
                // We found a starting point, fetch from there to current time
                //SYSTEM SET HERE
                loggingService.logAction("Found earliest data point for " + symbol +
                        " at " + earliestDataPoint.get().atZone(ZoneOffset.UTC));

                //SYSTEM SET HERE
                loggingService.logAction("Fetching all historical data for " +
                        symbol + " with periodic target time updates");
                // Fetch with dynamic end time updates (null means use current time with updates)
                fetchCandlesInRange(stock, symbol, earliestDataPoint.get(), null);
            } else {
                //SYSTEM SET HERE
                loggingService.logAction("Could not find any historical data for " + symbol);
            }
        }
    }

    /**
     * NEW METHOD: Handles saving a batch of candles and their aggregations in a single, short transaction.
     * @param stock The platform stock entity
     * @param candles The list of minute candles to save and aggregate
     */
    @Transactional
    public void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        List<MarketCandle> candlesToSave = new ArrayList<>();

        for (MarketCandle candle : candles) {
            // Check if candle already exists
            Optional<MarketCandle> existingCandle = candleRepository
                    .findByPlatformStockAndTimeframeAndTimestampEquals(
                            stock, candle.getTimeframe(), candle.getTimestamp());

            if (existingCandle.isPresent()) {
                // Update existing candle
                MarketCandle existing = existingCandle.get();
                existing.setOpenPrice(candle.getOpenPrice());
                existing.setHighPrice(candle.getHighPrice());
                existing.setLowPrice(candle.getLowPrice());
                existing.setClosePrice(candle.getClosePrice());
                existing.setVolume(candle.getVolume());
                candlesToSave.add(existing);
            } else {
                // Add new candle to save list
                candlesToSave.add(candle);
            }
        }

        // Save all candles (new and updated ones)
        if (!candlesToSave.isEmpty()) {
            candleRepository.saveAll(candlesToSave);
        }

        // Aggregate after saving
        aggregateCandles(stock, candlesToSave);
    }

    private void fetchCandlesInRange(PlatformStock stock, String symbol,
                                     Instant startInstant, Instant endInstant) {
        boolean useDynamicEnd = (endInstant == null);
        Instant targetEndTime = useDynamicEnd ? Instant.now() : endInstant;

        long startTime = startInstant.toEpochMilli();
        long endTime = targetEndTime.toEpochMilli();
        long currentStartTime = startTime;

        //SYSTEM SET HERE
        loggingService.logAction("Starting data fetch for " + symbol + " from " + startInstant.atZone(ZoneOffset.UTC) +
                " to " + targetEndTime.atZone(ZoneOffset.UTC) +
                (useDynamicEnd ? " (with dynamic end time)" : ""));
        int totalCandlesFetched = 0;
        int batchesFetched = 0;
        Instant lastTimeUpdate = Instant.now();

        while (currentStartTime < endTime) {
            try {
                if (useDynamicEnd && (batchesFetched % UPDATE_TARGET_TIME_EVERY_N_BATCHES == 0 ||
                        Duration.between(lastTimeUpdate, Instant.now()).toMinutes() >= UPDATE_TARGET_TIME_AFTER_MINUTES)) {
                    targetEndTime = Instant.now();
                    endTime = targetEndTime.toEpochMilli();
                    lastTimeUpdate = Instant.now();
                    loggingService.logAction("Updated target end time to " +
                            targetEndTime.atZone(ZoneOffset.UTC) + " after fetching " + batchesFetched + " batches");
                }

                // Step 1: Fetch from API (no transaction)
                ResponseEntity<String> response = apiClient.getKlines(
                        symbol, "1m", currentStartTime, null, MAX_CANDLES_PER_REQUEST);
                List<MarketCandle> candles = parseCandles(response.getBody(), stock);
                batchesFetched++;

                if (candles.isEmpty()) {
                    loggingService.logAction("No more candles found for " + symbol +
                            " after " + Instant.ofEpochMilli(currentStartTime).atZone(ZoneOffset.UTC));
                    break;
                }

                // Step 2: Save the fetched batch in a short, dedicated transaction
                saveCandleBatch(stock, candles);

                totalCandlesFetched += candles.size();

                if (totalCandlesFetched % 5000 == 0) {
                    loggingService.logAction("Fetched " + totalCandlesFetched + " candles so far for " + symbol);
                }

                // Step 3: Calculate next start time for the next API call
                MarketCandle lastCandle = candles.get(candles.size() - 1);
                currentStartTime = lastCandle.getTimestamp()
                        .plusMinutes(1)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();

                if (candles.size() < MAX_CANDLES_PER_REQUEST) {
                    loggingService.logAction("Received partial batch of candles (" +
                            candles.size() + "). Likely reached the end of available data.");
                    if (useDynamicEnd) {
                        targetEndTime = Instant.now();
                        if (currentStartTime < targetEndTime.toEpochMilli()) {
                            loggingService.logAction("Still need to fetch more recent data up to " +
                                    targetEndTime.atZone(ZoneOffset.UTC));
                            endTime = targetEndTime.toEpochMilli();
                            continue;
                        }
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
        loggingService.logAction("Completed data fetch for " + symbol + ". Total candles fetched: " + totalCandlesFetched);
    }

    /**
     * Finds the earliest available data point for a symbol using the week-by-week strategy
     *
     * @param symbol          The trading pair symbol
     * @param firstCheckpoint The current time when we started searching
     * @return Optional containing the earliest timestamp when data is available, or empty if none found
     */
    private Optional<Instant> findEarliestAvailableData(String symbol, Instant firstCheckpoint) {
        // Start from the checkpoint
        Instant checkPoint = firstCheckpoint;
        boolean foundGap = false;

        // Track the point where we last found data
        Instant lastDataPoint = null;
        // Go back week by week until we find a gap in data
        while (!foundGap) {
            // Move back one week
            checkPoint = checkPoint.minus(Duration.ofMinutes(WEEK_IN_MINUTES));
            //SYSTEM SET HERE
            loggingService.logAction("Checking if data exists for " + symbol +
                    " at week starting " + checkPoint.atZone(ZoneOffset.UTC));
            // Check if there are any candles in this week
            boolean hasCandles = checkForCandles(symbol, checkPoint,
                    checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES)));
            if (hasCandles) {
                // We found candles, update the last data point
                lastDataPoint = checkPoint;
            } else {
                // No candles in this week, let's check a wider range (±3.5 days)
                Instant expandedStart = checkPoint.minus(Duration.ofMinutes(HALF_WEEK_IN_MINUTES));
                Instant expandedEnd = checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES + HALF_WEEK_IN_MINUTES));

                //SYSTEM SET HERE
                loggingService.logAction("No data found in week. Checking expanded range from " +
                        expandedStart.atZone(ZoneOffset.UTC) + " to " +
                        expandedEnd.atZone(ZoneOffset.UTC));
                boolean hasExpandedCandles = checkForCandles(symbol, expandedStart, expandedEnd);

                if (hasExpandedCandles) {
                    // Found candles in the expanded range, need to narrow down
                    //SYSTEM SET HERE
                    loggingService.logAction("Found data in expanded range. Narrowing down...");
                    // Binary search to find the exact starting point would be ideal here
                    // For simplicity, we'll use the expanded start for now
                    lastDataPoint = expandedStart;
                } else {
                    // No candles even in expanded range, we've found our gap
                    //SYSTEM SET HERE
                    foundGap = true;
                    loggingService.logAction("Found data gap. Earliest data appears to be after " +
                            checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES + HALF_WEEK_IN_MINUTES))
                                    .atZone(ZoneOffset.UTC));
                }
            }
        }

        // If we found a data point, return it;
        // otherwise return empty
        if (lastDataPoint != null) {
            return Optional.of(lastDataPoint);
        } else if (!foundGap) {
            // We never found a gap (shouldn't happen without time limit)
            // But just in case, use the last checkpoint
            return Optional.of(checkPoint);
        }

        return Optional.empty();
    }

    /**
     * Checks if there are any candles in a given time range
     *
     * @param symbol    The trading pair symbol
     * @param startTime The start of the time range
     * @param endTime   The end of the time range
     * @return true if candles exist in this range, false otherwise
     */
    private boolean checkForCandles(String symbol, Instant startTime, Instant endTime) {
        try {
            // Make a small request just to check if data exists (limit=1)
            ResponseEntity<String> response = apiClient.getKlines(
                    symbol, "1m",
                    startTime.toEpochMilli(),
                    endTime.toEpochMilli(),
                    1);
            // Just get one candle if available

            // Parse the response
            JsonNode candlesArray = objectMapper.readTree(response.getBody());
            // If the array is not empty, candles exist in this range
            return !candlesArray.isEmpty();
        } catch (Exception e) {
            //SYSTEM SET HERE
            loggingService.logError("Error checking for candles: " + e.getMessage(), e);
            return false;
        }
    }

    private List<MarketCandle> parseCandles(String responseBody, PlatformStock stock) {
        List<MarketCandle> candles = new ArrayList<>();
        try {
            JsonNode candlesArray = objectMapper.readTree(responseBody);
            for (JsonNode candleNode : candlesArray) {
                // Binance candle format is an array:
                // [openTime, open, high, low, close, volume, closeTime, ...]

                long openTime = candleNode.get(0).asLong();
                String open = candleNode.get(1).asText();
                String high = candleNode.get(2).asText();
                String low = candleNode.get(3).asText();
                String close = candleNode.get(4).asText();
                String volume = candleNode.get(5).asText();
                // Create candle entity
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
            //SYSTEM SET HERE
            loggingService.logError("Error parsing candle data: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse candle data", e);
        }

        return candles;
    }

    private void aggregateCandles(PlatformStock stock, List<MarketCandle> minuteCandles) {
        if (minuteCandles.isEmpty()) return;
        // Aggregate to 5m timeframe
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M5, 5);
        // Aggregate to 15m timeframe
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M15, 15);
        // Aggregate to 1h timeframe
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H1, 60);
        // Aggregate to 4h timeframe
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H4, 240);
        // Aggregate to 1d timeframe
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.D1, 1440);
    }

    private void aggregateTimeframe(PlatformStock stock, List<MarketCandle> minuteCandles,
                                    MarketCandle.Timeframe timeframe, int minutes) {
        try {
            // Group candles by timeframe boundary
            Map<LocalDateTime, List<MarketCandle>> groupedCandles = new HashMap<>();
            for (MarketCandle candle : minuteCandles) {
                LocalDateTime timestamp = candle.getTimestamp();
                LocalDateTime timeframeStart = calculateTimeframeStart(timestamp, minutes);

                if (!groupedCandles.containsKey(timeframeStart)) {
                    groupedCandles.put(timeframeStart, new ArrayList<>());
                }

                groupedCandles.get(timeframeStart).add(candle);
            }

            // Process each group to create or update aggregated candles
            List<MarketCandle> candles = new ArrayList<>();
            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                List<MarketCandle> candlesInPeriod = entry.getValue();

                // Only process if we have candles in this period
                if (!candlesInPeriod.isEmpty()) {
                    // Check if an aggregate candle already exists
                    MarketCandle candle = candleRepository
                            .findByPlatformStockAndTimeframeAndTimestampEquals(
                                    stock, timeframe, timeframeStart)
                            .orElse(null);
                    if (candle == null) {
                        // Create new candle
                        candle = createAggregatedCandle(stock, timeframe, timeframeStart, candlesInPeriod);
                    } else {
                        // Update existing candle with new data
                        updateExistingCandle(candle, candlesInPeriod);
                    }

                    candles.add(candle);
                }
            }

            // Save all candles
            if (!candles.isEmpty()) {
                candleRepository.saveAll(candles);
                //SYSTEM SET HERE
                loggingService.logAction("Saved/updated " + candles.size() + " candles for timeframe " + timeframe);
            }
        } catch (Exception e) {
            //SYSTEM SET HERE
            loggingService.logError("Error aggregating candles to timeframe " +
                    timeframe + ": " + e.getMessage(), e);
        }
    }

    private void updateExistingCandle(MarketCandle existingCandle, List<MarketCandle> minuteCandles) {
        // Sort candles by timestamp to ensure correct order
        minuteCandles.sort(Comparator.comparing(MarketCandle::getTimestamp));
        // Update high if needed
        BigDecimal newHigh = minuteCandles.stream()
                .map(MarketCandle::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (newHigh.compareTo(existingCandle.getHighPrice()) > 0) {
            existingCandle.setHighPrice(newHigh);
        }

        // Update low if needed
        BigDecimal newLow = minuteCandles.stream()
                .map(MarketCandle::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (newLow.compareTo(existingCandle.getLowPrice()) < 0) {
            existingCandle.setLowPrice(newLow);
        }

        // Always update close to the latest
        existingCandle.setClosePrice(minuteCandles.get(minuteCandles.size() - 1).getClosePrice());
        // Don't update open (it should be from the first candle in the period)
        // For volume, we'd need to be careful about double-counting
        // This is a simplified approach - a more accurate solution would involve
        // tracking which minute candles have been included in the aggregate
    }

    private LocalDateTime calculateTimeframeStart(LocalDateTime time, int minutes) {
        // Calculate the start of the timeframe this minute belongs to
        int minuteOfDay = time.getHour() * 60 + time.getMinute();
        int timeframeIndex = minuteOfDay / minutes;

        return time
                .withHour((timeframeIndex * minutes) / 60)
                .withMinute((timeframeIndex * minutes) % 60)
                .withSecond(0)
                .withNano(0);
    }

    private MarketCandle createAggregatedCandle(PlatformStock stock, MarketCandle.Timeframe timeframe,
                                                LocalDateTime timeframeStart, List<MarketCandle> candles) {
        // First candle's open is our open
        BigDecimal open = candles.get(0).getOpenPrice();
        // Last candle's close is our close
        BigDecimal close = candles.get(candles.size() - 1).getClosePrice();
        // Find highest high and lowest low
        BigDecimal high = candles.stream()
                .map(MarketCandle::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal low = candles.stream()
                .map(MarketCandle::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        // Sum volumes
        BigDecimal volume = candles.stream()
                .map(MarketCandle::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Create the aggregated candle
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

    @Transactional
    public void processRealtimeCandle(JsonNode candleData, PlatformStock stock) {
        //SYSTEM SET HERE
        if (!userContextService.isAuthenticated()) {
            userContextService.setUser("SYSTEM", "SYSTEM");
        }
        try {
            System.out.println("=== PROCESSING REALTIME CANDLE START ===");
            System.out.println("Stock: " + stock.getStock().getStockName() + " (ID: " + stock.getPlatformStockId() + ")");
            // Extract candle fields from the websocket data
            JsonNode k = candleData.get("k");
            long openTime = k.get("t").asLong();
            String open = k.get("o").asText();
            String high = k.get("h").asText();
            String low = k.get("l").asText();
            String close = k.get("c").asText();
            String volume = k.get("v").asText();
            boolean isFinal = k.get("x").asBoolean();

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(openTime), ZoneOffset.UTC);
            System.out.println("Candle timestamp: " + timestamp);
            System.out.println("Candle isFinal: " + isFinal);
            System.out.println("Thread: " + Thread.currentThread().getName());
            // Check how many existing candles there are BEFORE the query
            System.out.println("About to query for existing candles...");
            System.out.println("Query parameters: stock=" + stock.getPlatformStockId() +
                    ", timeframe=M1, timestamp=" + timestamp);
            // Count existing candles first to see what we're dealing with
            try {
                List<MarketCandle> allMatching = candleRepository
                        .findAllByPlatformStockAndTimeframeAndTimestamp(
                                stock, MarketCandle.Timeframe.M1,
                                timestamp);

                System.out.println("FOUND " + allMatching.size() + " existing candles for this combination!");
                if (allMatching.size() > 1) {
                    System.out.println("DUPLICATE DETECTED! Details:");
                    for (int i = 0; i < allMatching.size(); i++) {
                        MarketCandle existing = allMatching.get(i);
                        System.out.println("  Candle " + i + ": ID=" + existing.getMarketCandleId() +
                                ", created=" + existing.getTimestamp());
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR during count query: " + e.getMessage());
            }

            // Now try the original query
            System.out.println("Now executing the original findBy...orElseGet query...");
            // Find existing candle or create new one
            MarketCandle candle = candleRepository
                    .findByPlatformStockAndTimeframeAndTimestampEquals(
                            stock, MarketCandle.Timeframe.M1, timestamp)
                    .orElseGet(() -> {

                        System.out.println("No existing candle found, creating new one");
                        MarketCandle newCandle = new MarketCandle();
                        newCandle.setPlatformStock(stock);

                        newCandle.setTimeframe(MarketCandle.Timeframe.M1);
                        newCandle.setTimestamp(timestamp);
                        return newCandle;
                    });
            System.out.println("Successfully got candle: ID=" + candle.getMarketCandleId());

            // Update candle data
            candle.setOpenPrice(new BigDecimal(open));
            candle.setHighPrice(new BigDecimal(high));
            candle.setLowPrice(new BigDecimal(low));
            candle.setClosePrice(new BigDecimal(close));
            candle.setVolume(new BigDecimal(volume));

            System.out.println("Updated candle data, about to save...");
            // Save the updated candle
            candle = candleRepository.save(candle);
            System.out.println("Saved candle successfully: ID=" + candle.getMarketCandleId());

            // If this is a completed candle, update aggregated timeframes
            if (isFinal) {
                System.out.println("Candle is final, aggregating...");
                List<MarketCandle> minuteCandles = new ArrayList<>();
                minuteCandles.add(candle);
                aggregateCandles(stock, minuteCandles);
                System.out.println("Aggregation completed");
            }

            System.out.println("=== PROCESSING REALTIME CANDLE END ===");
        } catch (Exception e) {
            System.out.println("=== EXCEPTION IN PROCESSING REALTIME CANDLE ===");
            System.out.println("Exception type: " + e.getClass().getSimpleName());
            System.out.println("Exception message: " + e.getMessage());
            System.out.println("Stock: " + (stock != null ? stock.getStock().getStockName() : "null"));
            System.out.println("Thread: " + Thread.currentThread().getName());

            if (e.getCause() != null) {
                System.out.println("Root cause: " + e.getCause().getClass().getSimpleName());
                System.out.println("Root cause message: " + e.getCause().getMessage());
            }

            e.printStackTrace();
            System.out.println("=== END EXCEPTION DEBUG ===");

            //SYSTEM SET HERE
            loggingService.logError("Error processing realtime candle: " + e.getMessage(), e);
        }
    }

    @Transactional
    private PlatformStock getOrCreateStock(String symbol) {
        // Find existing stock or create a new one if needed
        List<PlatformStock> stocks = stockRepository.findByPlatformPlatformNameAndStockStockName(
                PLATFORM_NAME, symbol);
        if (!stocks.isEmpty()) {
            return stocks.get(0);
        }

        // Create new stock entry - need to find or create Platform and Stock entities
        Platform platform = platformRepository.findByPlatformName(PLATFORM_NAME)
                .orElseThrow(() -> new RuntimeException("Platform not found: " + PLATFORM_NAME));

        Stock stock = stockRepositoryForStock.findByStockName(symbol)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setStockName(symbol);
                    return stockRepositoryForStock.save(newStock);
                });

        PlatformStock platformStock = new PlatformStock();
        platformStock.setPlatform(platform);
        platformStock.setStock(stock);
        return stockRepository.save(platformStock);
    }
}