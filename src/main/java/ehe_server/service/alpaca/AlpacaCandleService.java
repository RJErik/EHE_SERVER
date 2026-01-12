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
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class AlpacaCandleService implements AlpacaCandleServiceInterface {

    private static final String PLATFORM_NAME = "Alpaca";
    private static final int WEEK_IN_MINUTES = 7 * 24 * 60;
    private static final int HALF_WEEK_IN_MINUTES = WEEK_IN_MINUTES / 2;
    private static final int DECIMAL_SCALE = 8;

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

    // Rounds BigDecimal to 8 decimal places for database compatibility
    private BigDecimal scaleDecimal(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    // Syncs all available historical data for a symbol from earliest point to now
    @Override
    public void syncHistoricalData(String symbol) {
        userContextService.setUser("SYSTEM", "SYSTEM");
        PlatformStock stock = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(PLATFORM_NAME, symbol).getFirst();

        // Check for existing candles to determine start point
        MarketCandle latestCandle = marketCandleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

        ZonedDateTime startTime;
        ZonedDateTime initialEnd = ZonedDateTime.now(ZoneOffset.UTC);

        if (latestCandle != null) {
            // Resume from last known candle
            startTime = latestCandle.getTimestamp().atZone(ZoneOffset.UTC);
            loggingService.logAction("Found existing candles for " + symbol +
                    ". Latest at " + latestCandle.getTimestamp());
        } else {
            // Search backwards to find earliest available data
            loggingService.logAction("No existing candles for " + symbol +
                    ". Searching for earliest available data...");
            Optional<ZonedDateTime> earliest = findEarliestAvailableData(symbol, initialEnd);
            if (earliest.isEmpty()) {
                loggingService.logAction("No historical data available for " + symbol);
                return;
            }
            startTime = earliest.get();
        }

        // Main sync pass
        loggingService.logAction("Starting main sync for " + symbol);
        ZonedDateTime firstSyncEnd = fetchCandlesInRange(stock, symbol, startTime, initialEnd);

        // Catch-up sync for data generated during main sync
        loggingService.logAction("Running catch-up sync for " + symbol);
        ZonedDateTime finalEnd = ZonedDateTime.now(ZoneOffset.UTC);
        fetchCandlesInRange(stock, symbol, firstSyncEnd, finalEnd);

        loggingService.logAction("Sync complete for " + symbol);
    }

    // Searches backward week-by-week to find the earliest available data point
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
                // Expand search range to confirm gap is real
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

    // Checks if any candles exist in the specified time range
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

    // Fetches candles for a date range using pagination, returns completion timestamp
    private ZonedDateTime fetchCandlesInRange(PlatformStock stock, String symbol,
                                              ZonedDateTime startTime, ZonedDateTime endTime) {
        loggingService.logAction("Fetching " + symbol + " from " + startTime + " to " + endTime);

        int totalCandlesFetched = 0;
        String pageToken = null;

        try {
            // Paginate through all available data
            do {
                ResponseEntity<String> response = alpacaDataApiClient.getBars(
                        symbol, "1Min", startTime, endTime, pageToken);

                JsonNode responseData = objectMapper.readTree(response.getBody());

                // Parse response based on symbol type
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

                // Rate limiting between pages
                if (pageToken != null) {
                    Thread.sleep(100);
                }

            } while (pageToken != null);

            loggingService.logAction("Completed: " + totalCandlesFetched + " candles for " + symbol);

        } catch (Exception e) {
            loggingService.logError("Error fetching candles for " + symbol + ": " + e.getMessage(), e);
        }

        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    // Extracts next page token from response, handling null and empty values
    private String extractNextPageToken(JsonNode responseData) {
        if (!responseData.has("next_page_token")) {
            return null;
        }

        JsonNode tokenNode = responseData.get("next_page_token");

        if (tokenNode == null || tokenNode.isNull()) {
            return null;
        }

        String tokenValue = tokenNode.asText();

        if (tokenValue == null || tokenValue.isEmpty() || "null".equalsIgnoreCase(tokenValue)) {
            return null;
        }

        return tokenValue;
    }

    // Saves a batch of candles with deduplication and triggers aggregation
    @Transactional
    @Override
    public void saveCandleBatch(PlatformStock stock, List<MarketCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        // Deduplicate by timestamp within batch
        Map<LocalDateTime, MarketCandle> uniqueCandles = new LinkedHashMap<>();
        for (MarketCandle candle : candles) {
            LocalDateTime truncatedTimestamp = candle.getTimestamp()
                    .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
            candle.setTimestamp(truncatedTimestamp);
            uniqueCandles.put(truncatedTimestamp, candle);
        }

        loggingService.logAction("Upserting " + uniqueCandles.size() + " candles for stock ID: " + stock.getPlatformStockId());

        // Upsert each candle with scaled values
        int count = 0;
        for (MarketCandle candle : uniqueCandles.values()) {
            marketCandleRepository.upsertCandle(
                    stock.getPlatformStockId(),
                    candle.getTimeframe().getValue(),
                    candle.getTimestamp(),
                    scaleDecimal(candle.getOpenPrice()),
                    scaleDecimal(candle.getHighPrice()),
                    scaleDecimal(candle.getLowPrice()),
                    scaleDecimal(candle.getClosePrice()),
                    scaleDecimal(candle.getVolume())
            );

            count++;
        }

        loggingService.logAction("Completed upserting " + count + " candles");

        // Trigger aggregation to higher timeframes
        aggregateCandles(stock, new ArrayList<>(uniqueCandles.values()));
    }

    // Parses Alpaca bar JSON into MarketCandle entities
    private List<MarketCandle> parseCandles(JsonNode barsArray, PlatformStock stock) {
        List<MarketCandle> candles = new ArrayList<>();

        try {
            if (barsArray == null || !barsArray.isArray()) {
                return candles;
            }

            for (JsonNode barNode : barsArray) {
                // Extract fields from Alpaca format
                String timestamp = barNode.get("t").asText();
                String open = barNode.get("o").asText();
                String high = barNode.get("h").asText();
                String low = barNode.get("l").asText();
                String close = barNode.get("c").asText();
                String volume = barNode.get("v").asText();

                // Convert to UTC LocalDateTime
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
                LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();

                // Build candle entity with scaled values
                MarketCandle candle = new MarketCandle();
                candle.setPlatformStock(stock);
                candle.setTimeframe(MarketCandle.Timeframe.M1);
                candle.setTimestamp(localDateTime);
                candle.setOpenPrice(scaleDecimal(new BigDecimal(open)));
                candle.setHighPrice(scaleDecimal(new BigDecimal(high)));
                candle.setLowPrice(scaleDecimal(new BigDecimal(low)));
                candle.setClosePrice(scaleDecimal(new BigDecimal(close)));
                candle.setVolume(scaleDecimal(new BigDecimal(volume)));

                candles.add(candle);
            }
        } catch (Exception e) {
            loggingService.logError("Error parsing candle data: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse candle data", e);
        }

        return candles;
    }

    // Processes a real-time candle from WebSocket and triggers aggregation
    @Transactional
    @Override
    public void processRealtimeCandle(JsonNode candleData, PlatformStock stock) {
        if (!userContextService.isAuthenticated()) {
            userContextService.setUser("SYSTEM", "SYSTEM");
        }

        try {
            // Extract fields from WebSocket message
            String timestamp = candleData.get("t").asText();
            String open = candleData.get("o").asText();
            String high = candleData.get("h").asText();
            String low = candleData.get("l").asText();
            String close = candleData.get("c").asText();
            String volume = candleData.get("v").asText();

            // Convert to UTC LocalDateTime
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp);
            LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();

            // Find existing or create new candle
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

            // Update with scaled values
            candle.setOpenPrice(scaleDecimal(new BigDecimal(open)));
            candle.setHighPrice(scaleDecimal(new BigDecimal(high)));
            candle.setLowPrice(scaleDecimal(new BigDecimal(low)));
            candle.setClosePrice(scaleDecimal(new BigDecimal(close)));
            candle.setVolume(scaleDecimal(new BigDecimal(volume)));

            candle = marketCandleRepository.save(candle);

            // Trigger aggregation
            List<MarketCandle> minuteCandles = new ArrayList<>();
            minuteCandles.add(candle);
            aggregateCandles(stock, minuteCandles);

        } catch (Exception e) {
            loggingService.logError("Error processing realtime candle: " + e.getMessage(), e);
        }
    }

    // Aggregates minute candles to all higher timeframes
    private void aggregateCandles(PlatformStock stock, List<MarketCandle> minuteCandles) {
        if (minuteCandles.isEmpty()) return;

        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M5, 5);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.M15, 15);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H1, 60);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.H4, 240);
        aggregateTimeframe(stock, minuteCandles, MarketCandle.Timeframe.D1, 1440);
    }

    // Aggregates M1 candles to a specific higher timeframe
    private void aggregateTimeframe(PlatformStock stock, List<MarketCandle> minuteCandles,
                                    MarketCandle.Timeframe timeframe, int minutes) {
        try {
            // Group incoming candles by their timeframe period
            Map<LocalDateTime, List<MarketCandle>> groupedCandles = new HashMap<>();

            for (MarketCandle candle : minuteCandles) {
                LocalDateTime timestamp = candle.getTimestamp();
                LocalDateTime timeframeStart = calculateTimeframeStart(timestamp, minutes);
                groupedCandles.computeIfAbsent(timeframeStart, _ -> new ArrayList<>()).add(candle);
            }

            // Process each affected timeframe period
            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                LocalDateTime timeframeEnd = timeframeStart.plusMinutes(minutes);

                // Fetch all M1 candles in this period for accurate aggregation
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

                // Sort for correct open/close determination
                allCandlesInPeriod.sort(Comparator.comparing(MarketCandle::getTimestamp));

                // Calculate OHLCV from all source candles
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

                // Upsert aggregated candle with scaled values
                marketCandleRepository.upsertCandle(
                        stock.getPlatformStockId(),
                        timeframe.getValue(),
                        timeframeStart,
                        scaleDecimal(openPrice),
                        scaleDecimal(highPrice),
                        scaleDecimal(lowPrice),
                        scaleDecimal(closePrice),
                        scaleDecimal(volume)
                );
            }

        } catch (Exception e) {
            loggingService.logError("Error aggregating candles to timeframe " +
                    timeframe + ": " + e.getMessage(), e);
        }
    }

    // Calculates the start timestamp for a given timeframe period
    private LocalDateTime calculateTimeframeStart(LocalDateTime time, int minutes) {
        int minuteOfDay = time.getHour() * 60 + time.getMinute();
        int timeframeIndex = minuteOfDay / minutes;

        return time
                .withHour((timeframeIndex * minutes) / 60)
                .withMinute((timeframeIndex * minutes) % 60)
                .withSecond(0)
                .withNano(0);
    }

    // Determines if a symbol is crypto based on "/" presence
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}