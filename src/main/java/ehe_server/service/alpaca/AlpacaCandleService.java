package ehe_server.service.alpaca;

import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.service.intf.alpaca.AlpacaCandleServiceInterface;
import ehe_server.service.intf.alpaca.AlpacaDataApiClientInterface;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AlpacaCandleService implements AlpacaCandleServiceInterface {

    private static final String PLATFORM_NAME = "Alpaca";
    private static final int WEEK_IN_MINUTES = 7 * 24 * 60;
    private static final int HALF_WEEK_IN_MINUTES = WEEK_IN_MINUTES / 2;

    private final AlpacaDataApiClientInterface alpacaDataApiClient;
    private final MarketCandleRepository marketCandleRepository;
    private final PlatformStockRepository platformStockRepository;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextServiceInterface userContextService;

    public AlpacaCandleService(
            AlpacaDataApiClientInterface alpacaDataApiClient,
            MarketCandleRepository marketCandleRepository,
            PlatformStockRepository platformStockRepository,
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextServiceInterface userContextService) {
        this.alpacaDataApiClient = alpacaDataApiClient;
        this.marketCandleRepository = marketCandleRepository;
        this.platformStockRepository = platformStockRepository;
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    /**
     * Syncs historical data for a symbol
     * This will fetch all available historical data from the earliest point to now
     */
    @Override
    public void syncHistoricalData(String symbol) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        PlatformStock stock = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(PLATFORM_NAME, symbol).getFirst();

        MarketCandle latestCandle = marketCandleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

        ZonedDateTime startTime;
        ZonedDateTime initialEnd = ZonedDateTime.now(ZoneOffset.UTC);

        if (latestCandle != null) {
            startTime = latestCandle.getTimestamp().atZone(ZoneOffset.UTC);
            loggingService.logAction("Found existing candles for " + symbol +
                    ". Latest at " + latestCandle.getTimestamp());
        } else {
            loggingService.logAction("No existing candles for " + symbol +
                    ". Searching for earliest available data...");
            Optional<ZonedDateTime> earliest = findEarliestAvailableData(symbol, initialEnd);
            if (earliest.isEmpty()) {
                loggingService.logAction("No historical data available for " + symbol);
                return;
            }
            startTime = earliest.get();
        }

        // First sync (potentially long)
        loggingService.logAction("Starting main sync for " + symbol);
        ZonedDateTime firstSyncEnd = fetchCandlesInRange(stock, symbol, startTime, initialEnd);

        // Second sync (catches anything generated during first sync)
        loggingService.logAction("Running catch-up sync for " + symbol);
        ZonedDateTime finalEnd = ZonedDateTime.now(ZoneOffset.UTC);
        fetchCandlesInRange(stock, symbol, firstSyncEnd, finalEnd);

        loggingService.logAction("Sync complete for " + symbol);
    }

    /**
     * Searches backward week-by-week to find the earliest available data point.
     * Uses an expanding search when gaps are detected.
     */
    private Optional<ZonedDateTime> findEarliestAvailableData(String symbol, ZonedDateTime firstCheckpoint) {
        ZonedDateTime checkPoint = firstCheckpoint;
        ZonedDateTime lastDataPoint = null;
        boolean foundGap = false;

        while (!foundGap) {
            checkPoint = checkPoint.minusMinutes(WEEK_IN_MINUTES);

            boolean hasCandles = checkForCandles(symbol, checkPoint,
                    checkPoint.plusMinutes(WEEK_IN_MINUTES));

            if (hasCandles) {
                lastDataPoint = checkPoint;
            } else {
                // Expand search range to confirm gap
                ZonedDateTime expandedStart = checkPoint.minusMinutes(HALF_WEEK_IN_MINUTES);
                ZonedDateTime expandedEnd = checkPoint.plusMinutes(WEEK_IN_MINUTES + HALF_WEEK_IN_MINUTES);

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
    private boolean checkForCandles(String symbol, ZonedDateTime startTime, ZonedDateTime endTime) {
        try {
            ResponseEntity<String> response = alpacaDataApiClient.getBars(
                    symbol, "1Min", startTime, endTime, null);

            JsonNode responseData = objectMapper.readTree(response.getBody());

            // Handle different response formats for crypto vs stock
            if (isCryptoSymbol(symbol)) {
                JsonNode barsObject = responseData.get("bars");
                if (barsObject != null && barsObject.has(symbol)) {
                    return !barsObject.get(symbol).isEmpty();
                }
                return false;
            } else {
                JsonNode barsArray = responseData.get("bars");
                return barsArray != null && barsArray.isArray() && !barsArray.isEmpty();
            }
        } catch (Exception e) {
            loggingService.logError("Error checking for candles: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fetches candles for a specific date range using pagination
     * Returns the timestamp when fetching completed (for catch-up)
     */

    private ZonedDateTime fetchCandlesInRange(PlatformStock stock, String symbol,
                                              ZonedDateTime startTime, ZonedDateTime endTime) {
        loggingService.logAction("Fetching " + symbol + " from " + startTime + " to " + endTime);

        int totalCandlesFetched = 0;
        String pageToken = null;

        try {
            do {
                ResponseEntity<String> response = alpacaDataApiClient.getBars(
                        symbol, "1Min", startTime, endTime, pageToken);

                JsonNode responseData = objectMapper.readTree(response.getBody());

                List<MarketCandle> candles;
                if (isCryptoSymbol(symbol)) {
                    JsonNode barsObject = responseData.get("bars");
                    candles = (barsObject != null && barsObject.has(symbol))
                            ? parseCandles(barsObject.get(symbol), stock)
                            : new ArrayList<>();
                } else {
                    candles = parseCandles(responseData.get("bars"), stock);
                }

                saveCandleBatch(stock, candles);
                totalCandlesFetched += candles.size();

                pageToken = extractNextPageToken(responseData);

                if (pageToken != null) {
                    Thread.sleep(100);
                }

            } while (pageToken != null);

            loggingService.logAction("Completed: " + totalCandlesFetched + " candles for " + symbol);

        } catch (Exception e) {
            loggingService.logError("Error fetching candles for " + symbol + ": " + e.getMessage(), e);
        }

        return ZonedDateTime.now(ZoneOffset.UTC);  // Return completion time
    }


    /**
     * Safely extracts the next page token, handling JSON null properly
     */
    private String extractNextPageToken(JsonNode responseData) {
        if (!responseData.has("next_page_token")) {
            return null;
        }

        JsonNode tokenNode = responseData.get("next_page_token");

        // Check if the node is null, missing, or contains "null" string
        if (tokenNode == null || tokenNode.isNull()) {
            return null;
        }

        String tokenValue = tokenNode.asText();

        // Additional safety: check for empty or "null" string
        if (tokenValue == null || tokenValue.isEmpty() || "null".equalsIgnoreCase(tokenValue)) {
            return null;
        }

        return tokenValue;
    }


    /**
     * Saves a batch of candles and their aggregations in a single transaction
     */
    @Transactional
    @Override
    public void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        // Deduplicate within batch and truncate timestamps
        Map<LocalDateTime, MarketCandle> uniqueCandles = new LinkedHashMap<>();
        for (MarketCandle candle : candles) {
            LocalDateTime truncatedTimestamp = candle.getTimestamp()
                    .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            candle.setTimestamp(truncatedTimestamp);
            uniqueCandles.put(truncatedTimestamp, candle);
        }

        loggingService.logAction("Upserting " + uniqueCandles.size() + " candles for stock ID: " + stock.getPlatformStockId());

        // Use native upsert
        int count = 0;
        for (MarketCandle candle : uniqueCandles.values()) {
            marketCandleRepository.upsertCandle(
                    stock.getPlatformStockId(),
                    candle.getTimeframe().getValue(),
                    candle.getTimestamp(),
                    candle.getOpenPrice(),
                    candle.getHighPrice(),
                    candle.getLowPrice(),
                    candle.getClosePrice(),
                    candle.getVolume()
            );

            count++;
        }

        loggingService.logAction("Completed upserting " + count + " candles");

        // Aggregate after saving
        aggregateCandles(stock, new ArrayList<>(uniqueCandles.values()));
    }

    /**
     * Parses Alpaca bar data into MarketCandle entities
     * Alpaca format: { "t": "2021-02-01T16:01:00Z", "o": 133.32, "h": 133.74,
     *                  "l": 133.31, "c": 133.5, "v": 1000, "n": 10, "vw": 133.5 }
     */
    private List<MarketCandle> parseCandles(JsonNode barsArray, PlatformStock stock) {
        List<MarketCandle> candles = new ArrayList<>();

        try {
            if (barsArray == null || !barsArray.isArray()) {
                return candles;
            }

            for (JsonNode barNode : barsArray) {
                String timestamp = barNode.get("t").asText();
                String open = barNode.get("o").asText();
                String high = barNode.get("h").asText();
                String low = barNode.get("l").asText();
                String close = barNode.get("c").asText();
                String volume = barNode.get("v").asText();

                // Parse timestamp
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
                LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();

                // Create candle entity
                MarketCandle candle = new MarketCandle();
                candle.setPlatformStock(stock);
                candle.setTimeframe(MarketCandle.Timeframe.M1);
                candle.setTimestamp(localDateTime);
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
     * Processes a real-time candle from WebSocket
     */
    @Transactional
    @Override
    public void processRealtimeCandle(JsonNode candleData, PlatformStock stock) {
        if (!userContextService.isAuthenticated()) {
            userContextService.setUser("SYSTEM", "SYSTEM");
        }

        try {
            // Extract fields from Alpaca bar message
            // Format: {"T":"b","S":"AAPL","o":133.32,"h":133.74,"l":133.31,"c":133.5,"v":1000,"t":"2021-02-01T16:01:00Z"}
            String timestamp = candleData.get("t").asText();
            String open = candleData.get("o").asText();
            String high = candleData.get("h").asText();
            String low = candleData.get("l").asText();
            String close = candleData.get("c").asText();
            String volume = candleData.get("v").asText();

            // Parse timestamp
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
            LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();

            // Find existing candle or create new one
            MarketCandle candle = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampEquals(
                            stock, MarketCandle.Timeframe.M1, localDateTime)
                    .orElseGet(() -> {
                        MarketCandle newCandle = new MarketCandle();
                        newCandle.setPlatformStock(stock);
                        newCandle.setTimeframe(MarketCandle.Timeframe.M1);
                        newCandle.setTimestamp(localDateTime);
                        return newCandle;
                    });

            // Update candle data
            candle.setOpenPrice(new BigDecimal(open));
            candle.setHighPrice(new BigDecimal(high));
            candle.setLowPrice(new BigDecimal(low));
            candle.setClosePrice(new BigDecimal(close));
            candle.setVolume(new BigDecimal(volume));

            // Save the candle
            candle = marketCandleRepository.save(candle);

            // Aggregate to higher timeframes
            List<MarketCandle> minuteCandles = new ArrayList<>();
            minuteCandles.add(candle);
            aggregateCandles(stock, minuteCandles);

        } catch (Exception e) {
            loggingService.logError("Error processing realtime candle: " + e.getMessage(), e);
        }
    }

    /**
     * Aggregates minute candles to higher timeframes
     */
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
                groupedCandles.computeIfAbsent(timeframeStart, _ -> new ArrayList<>()).add(candle);
            }

            // Process each group
            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                List<MarketCandle> candlesInPeriod = entry.getValue();

                if (candlesInPeriod.isEmpty()) {
                    continue;
                }

                // Sort candles by timestamp for correct open/close
                candlesInPeriod.sort(Comparator.comparing(MarketCandle::getTimestamp));

                // Check if we need to merge with existing data
                Optional<MarketCandle> existingCandle = marketCandleRepository
                        .findByPlatformStockAndTimeframeAndTimestampEquals(
                                stock, timeframe, timeframeStart);

                BigDecimal openPrice;
                BigDecimal closePrice;
                BigDecimal highPrice;
                BigDecimal lowPrice;
                BigDecimal volume;

                if (existingCandle.isPresent()) {
                    // Merge with existing candle
                    MarketCandle existing = existingCandle.get();

                    // Keep existing open if it's earlier data
                    openPrice = existing.getOpenPrice();

                    // Use new close (latest data)
                    closePrice = candlesInPeriod.getLast().getClosePrice();

                    // Max of existing and new highs
                    BigDecimal newHigh = candlesInPeriod.stream()
                            .map(MarketCandle::getHighPrice)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    highPrice = existing.getHighPrice().max(newHigh);

                    // Min of existing and new lows
                    BigDecimal newLow = candlesInPeriod.stream()
                            .map(MarketCandle::getLowPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    lowPrice = existing.getLowPrice().min(newLow);

                    // Add volumes
                    BigDecimal newVolume = candlesInPeriod.stream()
                            .map(MarketCandle::getVolume)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    volume = existing.getVolume().add(newVolume);
                } else {
                    // Create new aggregation from scratch
                    openPrice = candlesInPeriod.getFirst().getOpenPrice();
                    closePrice = candlesInPeriod.getLast().getClosePrice();
                    highPrice = candlesInPeriod.stream()
                            .map(MarketCandle::getHighPrice)
                            .max(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    lowPrice = candlesInPeriod.stream()
                            .map(MarketCandle::getLowPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);
                    volume = candlesInPeriod.stream()
                            .map(MarketCandle::getVolume)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                // Use upsert - can never fail on duplicates!
                marketCandleRepository.upsertCandle(
                        stock.getPlatformStockId(),
                        timeframe.getValue(),
                        timeframeStart,
                        openPrice,
                        highPrice,
                        lowPrice,
                        closePrice,
                        volume
                );
            }

        } catch (Exception e) {
            loggingService.logError("Error aggregating candles to timeframe " +
                    timeframe + ": " + e.getMessage(), e);
        }
    }

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
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}