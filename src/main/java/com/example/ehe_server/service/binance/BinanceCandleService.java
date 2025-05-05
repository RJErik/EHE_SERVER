package com.example.ehe_server.service.binance;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.AuditContextService;
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
public class BinanceCandleService {
    private static final String PLATFORM_NAME = "Binance";
    private static final int MAX_CANDLES_PER_REQUEST = 1000;
    private static final int WEEK_IN_MINUTES = 7 * 24 * 60;
    private static final int HALF_WEEK_IN_MINUTES = WEEK_IN_MINUTES / 2;

    // How often to update the target end time during fetching
    private static final int UPDATE_TARGET_TIME_EVERY_N_BATCHES = 5;
    private static final int UPDATE_TARGET_TIME_AFTER_MINUTES = 15;

    private final BinanceApiClient apiClient;
    private final MarketCandleRepository candleRepository;
    private final PlatformStockRepository stockRepository;
    private final ObjectMapper objectMapper;
    private final AuditContextService auditContextService;
    private final LoggingServiceInterface loggingService;

    public BinanceCandleService(
            BinanceApiClient apiClient,
            MarketCandleRepository candleRepository,
            PlatformStockRepository stockRepository,
            ObjectMapper objectMapper,
            AuditContextService auditContextService,
            LoggingServiceInterface loggingService) {
        this.apiClient = apiClient;
        this.candleRepository = candleRepository;
        this.stockRepository = stockRepository;
        this.objectMapper = objectMapper;
        this.auditContextService = auditContextService;
        this.loggingService = loggingService;
    }

    @Transactional
    public void syncHistoricalData(String symbol) {
        PlatformStock stock = getOrCreateStock(symbol);

        // Find latest existing candle
        MarketCandle latestCandle = candleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

        if (latestCandle != null) {
            // We have existing data, start from the latest candle
            loggingService.logAction(null, "System", "Found existing candles for " + symbol +
                    ". Latest candle at " + latestCandle.getTimestamp());

            // Start from the next minute after latest candle
            Instant startInstant = latestCandle.getTimestamp()
                    .plusMinutes(1)
                    .toInstant(ZoneOffset.UTC);

            loggingService.logAction(null, "System", "Updating candles for " + symbol +
                    " from " + startInstant.atZone(ZoneOffset.UTC) +
                    " to present (with periodic target time updates)");

            // Fetch with dynamic end time updates (null means use current time with updates)
            fetchCandlesInRange(stock, symbol, startInstant, null);
        } else {
            // No existing data, need to find how far back data is available
            loggingService.logAction(null, "System", "No existing candles for " + symbol +
                    ". Finding earliest available data...");

            // Find the earliest available data using the backward search strategy
            Instant firstCheckpoint = Instant.now();
            Optional<Instant> earliestDataPoint = findEarliestAvailableData(symbol, firstCheckpoint);

            if (earliestDataPoint.isPresent()) {
                // We found a starting point, fetch from there to current time
                loggingService.logAction(null, "System", "Found earliest data point for " + symbol +
                        " at " + earliestDataPoint.get().atZone(ZoneOffset.UTC));

                loggingService.logAction(null, "System", "Fetching all historical data for " +
                        symbol + " with periodic target time updates");

                // Fetch with dynamic end time updates (null means use current time with updates)
                fetchCandlesInRange(stock, symbol, earliestDataPoint.get(), null);
            } else {
                loggingService.logAction(null, "System", "Could not find any historical data for " + symbol);
            }
        }
    }

    /**
     * Finds the earliest available data point for a symbol using the week-by-week strategy
     * @param symbol The trading pair symbol
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

            loggingService.logAction(null, "System", "Checking if data exists for " + symbol +
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

                loggingService.logAction(null, "System", "No data found in week. Checking expanded range from " +
                        expandedStart.atZone(ZoneOffset.UTC) + " to " +
                        expandedEnd.atZone(ZoneOffset.UTC));

                boolean hasExpandedCandles = checkForCandles(symbol, expandedStart, expandedEnd);

                if (hasExpandedCandles) {
                    // Found candles in the expanded range, need to narrow down
                    loggingService.logAction(null, "System", "Found data in expanded range. Narrowing down...");

                    // Binary search to find the exact starting point would be ideal here
                    // For simplicity, we'll use the expanded start for now
                    lastDataPoint = expandedStart;
                } else {
                    // No candles even in expanded range, we've found our gap
                    foundGap = true;
                    loggingService.logAction(null, "System", "Found data gap. Earliest data appears to be after " +
                            checkPoint.plus(Duration.ofMinutes(WEEK_IN_MINUTES + HALF_WEEK_IN_MINUTES))
                                    .atZone(ZoneOffset.UTC));
                }
            }
        }

        // If we found a data point, return it; otherwise return empty
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
     * @param symbol The trading pair symbol
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return true if candles exist in this range, false otherwise
     */
    private boolean checkForCandles(String symbol, Instant startTime, Instant endTime) {
        try {
            // Make a small request just to check if data exists (limit=1)
            ResponseEntity<String> response = apiClient.getKlines(
                    symbol, "1m",
                    startTime.toEpochMilli(),
                    endTime.toEpochMilli(),
                    1); // Just get one candle if available

            // Parse the response
            JsonNode candlesArray = objectMapper.readTree(response.getBody());

            // If the array is not empty, candles exist in this range
            return candlesArray.size() > 0;
        } catch (Exception e) {
            loggingService.logError(null, "System", "Error checking for candles: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fetches all candles in a given time range, handling pagination automatically
     * Updates the target end time periodically to ensure we get all the latest data
     *
     * @param stock The platform stock entity
     * @param symbol The trading pair symbol
     * @param startInstant The start time
     * @param endInstant The end time (can be null to use current time with periodic updates)
     */
    private void fetchCandlesInRange(PlatformStock stock, String symbol,
                                     Instant startInstant, Instant endInstant) {
        // Get a fresh current time at the start of the fetch
        boolean useDynamicEnd = (endInstant == null);
        Instant targetEndTime = useDynamicEnd ? Instant.now() : endInstant;

        long startTime = startInstant.toEpochMilli();
        long endTime = targetEndTime.toEpochMilli();
        long currentStartTime = startTime;

        loggingService.logAction(null, "System", "Starting data fetch for " + symbol +
                " from " + startInstant.atZone(ZoneOffset.UTC) +
                " to " + targetEndTime.atZone(ZoneOffset.UTC) +
                (useDynamicEnd ? " (with dynamic end time)" : ""));

        int totalCandlesFetched = 0;
        int batchesFetched = 0;
        Instant lastTimeUpdate = Instant.now();

        // Fetch data in chunks until we reach the end time
        while (currentStartTime < endTime) {
            try {
                // Check if we need to update our target end time
                if (useDynamicEnd && (batchesFetched % UPDATE_TARGET_TIME_EVERY_N_BATCHES == 0 ||
                        Duration.between(lastTimeUpdate, Instant.now()).toMinutes() >= UPDATE_TARGET_TIME_AFTER_MINUTES)) {
                    // Get a fresh current time
                    targetEndTime = Instant.now();
                    endTime = targetEndTime.toEpochMilli();
                    lastTimeUpdate = Instant.now();

                    loggingService.logAction(null, "System", "Updated target end time to " +
                            targetEndTime.atZone(ZoneOffset.UTC) + " after fetching " + batchesFetched + " batches");
                }

                // Fetch the next batch of candles
                ResponseEntity<String> response = apiClient.getKlines(
                        symbol, "1m", currentStartTime, null, MAX_CANDLES_PER_REQUEST);

                List<MarketCandle> candles = parseCandles(response.getBody(), stock);
                batchesFetched++;

                if (candles.isEmpty()) {
                    loggingService.logAction(null, "System", "No more candles found for " + symbol +
                            " after " + Instant.ofEpochMilli(currentStartTime).atZone(ZoneOffset.UTC));
                    break;
                }

                candleRepository.saveAll(candles);
                totalCandlesFetched += candles.size();

                // Log progress periodically
                if (totalCandlesFetched % 5000 == 0) {
                    loggingService.logAction(null, "System", "Fetched " + totalCandlesFetched +
                            " candles so far for " + symbol);
                }

                // Aggregate this batch of candles
                aggregateCandles(stock, candles);

                // Calculate next start time
                MarketCandle lastCandle = candles.get(candles.size() - 1);
                currentStartTime = lastCandle.getTimestamp()
                        .plusMinutes(1)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();

                // If we didn't get a full batch, we've reached the end of available data
                if (candles.size() < MAX_CANDLES_PER_REQUEST) {
                    loggingService.logAction(null, "System", "Received partial batch of candles (" +
                            candles.size() + "). Likely reached the end of available data.");

                    // If using dynamic end time, do one final check with a fresh end time
                    if (useDynamicEnd) {
                        targetEndTime = Instant.now();

                        // If we're still not at the updated current time, continue fetching
                        if (currentStartTime < targetEndTime.toEpochMilli()) {
                            loggingService.logAction(null, "System", "Still need to fetch more recent data up to " +
                                    targetEndTime.atZone(ZoneOffset.UTC));
                            endTime = targetEndTime.toEpochMilli();
                            continue;
                        }
                    }
                    break;
                }
            } catch (Exception e) {
                loggingService.logError(null, "System", "Error fetching candles: " + e.getMessage(), e);
                // Add a small delay before retrying
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        loggingService.logAction(null, "System", "Completed data fetch for " + symbol +
                ". Total candles fetched: " + totalCandlesFetched);
    }

    private List<MarketCandle> parseCandles(String responseBody, PlatformStock stock) {
        auditContextService.setCurrentUser("System");
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
            loggingService.logError(null, "System", "Error parsing candle data: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse candle data", e);
        }

        return candles;
    }

    private void aggregateCandles(PlatformStock stock, List<MarketCandle> minuteCandles) {
        auditContextService.setCurrentUser("System");
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
        auditContextService.setCurrentUser("System");
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

            // Process each group to create aggregated candles
            List<MarketCandle> aggregatedCandles = new ArrayList<>();

            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                List<MarketCandle> candlesInPeriod = entry.getValue();

                // Only create aggregated candle if we have enough data
                if (candlesInPeriod.size() > 0) {
                    MarketCandle aggregated = createAggregatedCandle(
                            stock, timeframe, timeframeStart, candlesInPeriod);
                    aggregatedCandles.add(aggregated);
                }
            }

            // Save aggregated candles
            if (!aggregatedCandles.isEmpty()) {
                candleRepository.saveAll(aggregatedCandles);
                loggingService.logAction(null, "System", "Saved " + aggregatedCandles.size() +
                        " aggregated candles for timeframe " + timeframe);
            }
        } catch (Exception e) {
            loggingService.logError(null, "System", "Error aggregating candles to timeframe " +
                    timeframe + ": " + e.getMessage(), e);
        }
    }

    private LocalDateTime calculateTimeframeStart(LocalDateTime time, int minutes) {
        auditContextService.setCurrentUser("System");
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
        auditContextService.setCurrentUser("System");
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
        auditContextService.setCurrentUser("System");
        try {
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

            // Find existing candle or create new one
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

            // Update candle data
            candle.setOpenPrice(new BigDecimal(open));
            candle.setHighPrice(new BigDecimal(high));
            candle.setLowPrice(new BigDecimal(low));
            candle.setClosePrice(new BigDecimal(close));
            candle.setVolume(new BigDecimal(volume));

            // Save the updated candle
            candle = candleRepository.save(candle);

            // If this is a completed candle, update aggregated timeframes
            if (isFinal) {
                List<MarketCandle> minuteCandles = new ArrayList<>();
                minuteCandles.add(candle);
                aggregateCandles(stock, minuteCandles);
            }
        } catch (Exception e) {
            loggingService.logError(null, "System", "Error processing realtime candle: " + e.getMessage(), e);
        }
    }

    private PlatformStock getOrCreateStock(String symbol) {
        auditContextService.setCurrentUser("System");
        // Find existing stock or create a new one if needed
        List<PlatformStock> stocks = stockRepository.findByPlatformNameAndStockSymbol(
                PLATFORM_NAME, symbol);

        if (!stocks.isEmpty()) {
            return stocks.get(0);
        }

        // Create new stock entry
        PlatformStock stock = new PlatformStock();
        stock.setPlatformName(PLATFORM_NAME);
        stock.setStockSymbol(symbol);
        return stockRepository.save(stock);
    }
}
