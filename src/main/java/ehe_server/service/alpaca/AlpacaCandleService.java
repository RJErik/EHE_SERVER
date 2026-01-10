package ehe_server.service.alpaca;

import ehe_server.entity.MarketCandle;
import ehe_server.entity.Platform;
import ehe_server.entity.PlatformStock;
import ehe_server.entity.Stock;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.PlatformRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.repository.StockRepository;
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
    private final PlatformRepository platformRepository;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;
    private final UserContextServiceInterface userContextService;

    public AlpacaCandleService(
            AlpacaDataApiClientInterface alpacaDataApiClient,
            MarketCandleRepository marketCandleRepository,
            PlatformStockRepository platformStockRepository,
            PlatformRepository platformRepository,
            StockRepository stockRepository,
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService,
            UserContextServiceInterface userContextService) {
        this.alpacaDataApiClient = alpacaDataApiClient;
        this.marketCandleRepository = marketCandleRepository;
        this.platformStockRepository = platformStockRepository;
        this.platformRepository = platformRepository;
        this.stockRepository = stockRepository;
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
        PlatformStock stock = getOrCreateStock(symbol);

        // Find latest existing candle
        MarketCandle latestCandle = marketCandleRepository
                .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        stock, MarketCandle.Timeframe.M1);

        if (latestCandle != null) {
            // We have existing data, update from latest candle to now
            loggingService.logAction("Found existing candles for " + symbol +
                    ". Latest candle at " + latestCandle.getTimestamp());

            ZonedDateTime startTime = latestCandle.getTimestamp()
                    .atZone(ZoneOffset.UTC);
            ZonedDateTime endTime = ZonedDateTime.now(ZoneOffset.UTC);

            loggingService.logAction("Updating candles for " + symbol + " from " +
                    startTime + " to " + endTime);

            fetchCandlesInRange(stock, symbol, startTime, endTime);
        } else {
            // No existing data - search backward to find earliest available data
            loggingService.logAction("No existing candles for " + symbol +
                    ". Searching for earliest available data...");

            ZonedDateTime firstCheckpoint = ZonedDateTime.now(ZoneOffset.UTC);
            Optional<ZonedDateTime> earliestDataPoint = findEarliestAvailableData(symbol, firstCheckpoint);

            if (earliestDataPoint.isPresent()) {
                loggingService.logAction("Fetching " + symbol + " from " +
                        earliestDataPoint.get() + " to present");
                fetchCandlesInRange(stock, symbol, earliestDataPoint.get(),
                        ZonedDateTime.now(ZoneOffset.UTC));
            } else {
                loggingService.logAction("No historical data available for " + symbol);
            }
        }
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
     */
    private void fetchCandlesInRange(PlatformStock stock, String symbol,
                                     ZonedDateTime startTime, ZonedDateTime endTime) {
        loggingService.logAction("Starting data fetch for " + symbol + " from " +
                startTime + " to " + endTime);

        int totalCandlesFetched = 0;
        int pagesFetched = 0;
        String pageToken = null;

        try {
            do {
                // Fetch a page of data
                ResponseEntity<String> response = alpacaDataApiClient.getBars(
                        symbol, "1Min", startTime, endTime, pageToken);

                JsonNode responseData = objectMapper.readTree(response.getBody());

                // Parse candles based on whether it's crypto or stock
                List<MarketCandle> candles;
                if (isCryptoSymbol(symbol)) {
                    // Crypto response format: { "bars": { "BTC/USD": [...] }, "next_page_token": "..." }
                    JsonNode barsObject = responseData.get("bars");
                    if (barsObject != null && barsObject.has(symbol)) {
                        candles = parseCandles(barsObject.get(symbol), stock);
                    } else {
                        candles = new ArrayList<>();
                    }
                } else {
                    // Stock response format: { "bars": [...], "next_page_token": "..." }
                    candles = parseCandles(responseData.get("bars"), stock);
                }

                pagesFetched++;

                if (candles.isEmpty()) {
                    loggingService.logAction("No more candles found for " + symbol);
                    break;
                }

                // Save the batch in a separate transaction
                saveCandleBatch(stock, candles);
                totalCandlesFetched += candles.size();

                if (totalCandlesFetched % 5000 == 0) {
                    loggingService.logAction("Fetched " + totalCandlesFetched +
                            " candles so far for " + symbol);
                }

                // Get next page token
                pageToken = responseData.has("next_page_token")
                        ? responseData.get("next_page_token").asText()
                        : null;

                // Add a small delay between pages to be nice to the API
                if (pageToken != null) {
                    Thread.sleep(100);
                }

            } while (pageToken != null);

            loggingService.logAction("Completed data fetch for " + symbol +
                    ". Total candles fetched: " + totalCandlesFetched +
                    " in " + pagesFetched + " pages");

        } catch (Exception e) {
            loggingService.logError("Error fetching candles for " + symbol + ": " +
                    e.getMessage(), e);
        }
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

        List<MarketCandle> candlesToSave = new ArrayList<>();

        for (MarketCandle candle : candles) {
            // Check if candle already exists
            Optional<MarketCandle> existingCandle = marketCandleRepository
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
        marketCandleRepository.saveAll(candlesToSave);

        // Aggregate after saving
        aggregateCandles(stock, candlesToSave);
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

            // Process each group to create or update aggregated candles
            List<MarketCandle> candles = new ArrayList<>();

            for (Map.Entry<LocalDateTime, List<MarketCandle>> entry : groupedCandles.entrySet()) {
                LocalDateTime timeframeStart = entry.getKey();
                List<MarketCandle> candlesInPeriod = entry.getValue();

                if (!candlesInPeriod.isEmpty()) {
                    MarketCandle candle = marketCandleRepository
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
                marketCandleRepository.saveAll(candles);
                loggingService.logAction("Saved/updated " + candles.size() +
                        " candles for timeframe " + timeframe);
            }
        } catch (Exception e) {
            loggingService.logError("Error aggregating candles to timeframe " +
                    timeframe + ": " + e.getMessage(), e);
        }
    }

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

    private LocalDateTime calculateTimeframeStart(LocalDateTime time, int minutes) {
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

    private PlatformStock getOrCreateStock(String symbol) {
        // Try to find existing platform stock
        List<PlatformStock> stocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(
                PLATFORM_NAME, symbol);

        if (!stocks.isEmpty()) {
            return stocks.getFirst();
        }

        // Create new stock entry - need to find or create Platform and Stock entities
        Platform platform = platformRepository.findByPlatformName(PLATFORM_NAME)
                .orElseThrow(() -> new RuntimeException("Platform not found: " + PLATFORM_NAME));

        Stock stock = stockRepository.findByStockSymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));

        PlatformStock platformStock = new PlatformStock();
        platformStock.setPlatform(platform);
        platformStock.setStock(stock);
        return platformStockRepository.save(platformStock);
    }

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }
}