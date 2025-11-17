package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse;
import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.MarketCandle.Timeframe;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.MarketCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MarketCandleService implements MarketCandleServiceInterface {

    private final MarketCandleRepository marketCandleRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MarketCandleService(
            MarketCandleRepository marketCandleRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService) {
        this.marketCandleRepository = marketCandleRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        System.out.println("=== MarketCandleService initialized at " + LocalDateTime.now().format(formatter) + " ===");
    }

    @Override
    public CandleDataResponse getCandleData(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        System.out.println("\n### EXECUTING getCandleData() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);
        System.out.println("Date range: " +
                (startDate != null ? startDate.format(formatter) : "null") + " to " +
                (endDate != null ? endDate.format(formatter) : "null"));

        CandleDataResponse response = new CandleDataResponse();
        response.setPlatformName(platformName);
        response.setStockSymbol(stockSymbol);
        response.setTimeframe(timeframeStr);

        try {
            // Find the platform stock
            System.out.println("Querying platform stocks database for platform: " + platformName);
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);
            System.out.println("Found " + platformStocks.size() + " platform stocks with name: " + platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock symbol '" + stockSymbol + "' not found for platform '" + platformName + "'");
                response.setSuccess(false);
                response.setMessage("Platform or stock not found");
                return response;
            }

            PlatformStock stock = platformStockOpt.get();
            System.out.println("Found matching stock: " + stock.getStockSymbol() + " (ID: " + stock.getPlatformStockId() + ")");

            // Parse timeframe
            Timeframe timeframe;
            try {
                System.out.println("Parsing timeframe string: " + timeframeStr);
                timeframe = parseTimeframe(timeframeStr);
                System.out.println("Successfully parsed timeframe to: " + timeframe);
                System.out.println("Will query database with timeframe: " + timeframe + " for data between " +
                        startDate.format(formatter) + " and " + endDate.format(formatter));
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                response.setSuccess(false);
                response.setMessage("Invalid timeframe: " + timeframeStr);
                return response;
            }

            // Fetch candles
            System.out.println("Fetching candles from database with params:");
            System.out.println("- Platform Stock ID: " + stock.getPlatformStockId());
            System.out.println("- Timeframe: " + timeframe);
            System.out.println("- Start Date: " + startDate.format(formatter));
            System.out.println("- End Date: " + endDate.format(formatter));

            long startTime = System.currentTimeMillis();
            List<MarketCandle> candles = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                            stock, timeframe, startDate, endDate);
            long queryTime = System.currentTimeMillis() - startTime;

            System.out.println("Database query completed in " + queryTime + "ms");
            System.out.println("Retrieved " + candles.size() + " candles from database for timeframe: " + timeframe);

            if (candles.isEmpty()) {
                System.out.println("WARNING: No candle data found for the specified criteria");
            } else {
                System.out.println("First candle: " +
                        candles.get(0).getTimestamp().format(formatter) + " | O:" +
                        candles.get(0).getOpenPrice() + " H:" + candles.get(0).getHighPrice() +
                        " L:" + candles.get(0).getLowPrice() + " C:" + candles.get(0).getClosePrice());

                System.out.println("Last candle: " +
                        candles.get(candles.size() - 1).getTimestamp().format(formatter) + " | O:" +
                        candles.get(candles.size() - 1).getOpenPrice() + " H:" + candles.get(candles.size() - 1).getHighPrice() +
                        " L:" + candles.get(candles.size() - 1).getLowPrice() + " C:" + candles.get(candles.size() - 1).getClosePrice());
            }

            // Convert to DTO
            System.out.println("Converting " + candles.size() + " candles to DTO objects");
            List<CandleData> candleDTOs = candles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            response.setSuccess(true);
            response.setCandles(candleDTOs);
            response.setLastUpdateTime(LocalDateTime.now());

            System.out.println("SUCCESS: Returning " + candleDTOs.size() + " candles for " +
                    stockSymbol + " (" + platformName + ") with timeframe " + timeframeStr);

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR in getCandleData: " + e.getMessage());
            e.printStackTrace();
            response.setSuccess(false);
            response.setMessage("Error fetching candle data: " + e.getMessage());
            loggingService.logError("Error fetching candle data: " + e.getMessage(), e);
        }

        System.out.println("getCandleData() completed at " + LocalDateTime.now().format(formatter));
        if (response.isSuccess()) {
            System.out.println(">>> SENT RESPONSE WITH " + response.getCandles().size() + " CANDLES FOR TIMEFRAME " +
                    response.getTimeframe() + " <<<");
            System.out.println(">>> DATA RANGE: " +
                    (response.getCandles().isEmpty() ? "EMPTY" :
                            response.getCandles().get(0).getTimestamp().format(formatter) + " to " +
                                    response.getCandles().get(response.getCandles().size() - 1).getTimestamp().format(formatter)) + " <<<");
        } else {
            System.out.println(">>> SENT ERROR RESPONSE: " + response.getMessage() + " <<<");
        }

        return response;
    }

    @Override
    public List<CandleData> getUpdatedCandles(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime lastCheckTime) {

        System.out.println("\n### EXECUTING getUpdatedCandles() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);
        System.out.println("Date range: " +
                (startDate != null ? startDate.format(formatter) : "null") + " to " +
                (endDate != null ? endDate.format(formatter) : "null"));
        System.out.println("Last check time: " +
                (lastCheckTime != null ? lastCheckTime.format(formatter) : "null"));

        try {
            // Find the platform stock
            System.out.println("Searching for platform: " + platformName + ", stock: " + stockSymbol);
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);
            System.out.println("Found " + platformStocks.size() + " platform stocks");

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock '" + stockSymbol + "' not found for platform '" + platformName + "'");
                return new ArrayList<>();
            }

            PlatformStock stock = platformStockOpt.get();
            System.out.println("Found matching stock: " + stock.getStockSymbol() + " (ID: " + stock.getPlatformStockId() + ")");

            // Parse timeframe
            System.out.println("Parsing timeframe: " + timeframeStr);
            Timeframe timeframe;
            try {
                timeframe = parseTimeframe(timeframeStr);
                System.out.println("Successfully parsed timeframe to: " + timeframe);
                System.out.println("Looking for updates for timeframe: " + timeframe +
                        " after " + lastCheckTime.format(formatter) +
                        " and before/at " + endDate.format(formatter));
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                return new ArrayList<>();
            }

            // ✅ FIXED: Fetch candles that are AFTER lastCheckTime but WITHIN the date range
            System.out.println("Querying database for candles after " + lastCheckTime.format(formatter) +
                    " and before/at " + endDate.format(formatter));

            List<MarketCandle> updatedCandles = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                            stock,
                            timeframe,
                            lastCheckTime,
                            endDate);

            System.out.println("Database returned " + updatedCandles.size() + " candles in the range");

            // Filter out the candle at lastCheckTime (we want AFTER, not including)
            List<MarketCandle> newCandles = updatedCandles.stream()
                    .filter(candle -> candle.getTimestamp().isAfter(lastCheckTime))
                    .collect(Collectors.toList());

            if (newCandles.isEmpty()) {
                System.out.println("No new candles found since last check time within the date range");
                System.out.println(">>> RETURNING 0 CANDLES FOR TIMEFRAME " + timeframeStr + " <<<");
                return new ArrayList<>();
            }

            System.out.println("Found " + newCandles.size() + " new candles within the date range");
            System.out.println("First new candle: " + newCandles.get(0).getTimestamp().format(formatter));
            System.out.println("Last new candle: " + newCandles.get(newCandles.size() - 1).getTimestamp().format(formatter));

            // Log all candle timestamps for debugging
            StringBuilder candleTimestamps = new StringBuilder();
            for (MarketCandle candle : newCandles) {
                if (candleTimestamps.length() > 0) {
                    candleTimestamps.append(", ");
                }
                candleTimestamps.append(candle.getTimestamp().format(formatter));
            }
            System.out.println("New candle timestamps: [" + candleTimestamps.toString() + "]");

            List<CandleData> result = newCandles.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            System.out.println(">>> RETURNING " + result.size() + " UPDATED CANDLES FOR TIMEFRAME " + timeframeStr + " <<<");
            return result;

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR in getUpdatedCandles: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error fetching updated candles: " + e.getMessage(), e);
            System.out.println(">>> RETURNING 0 CANDLES DUE TO ERROR FOR TIMEFRAME " + timeframeStr + " <<<");
            return new ArrayList<>();
        }
    }

    @Override
    public CandleData getModifiedLatestCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime lastCandleTimestamp,
            BigDecimal lastCandleOpen,
            BigDecimal lastCandleHigh,
            BigDecimal lastCandleLow,
            BigDecimal lastCandleClose,
            BigDecimal lastCandleVolume,
            LocalDateTime endDate) {  // ✅ Added endDate parameter

        System.out.println("\n### EXECUTING getModifiedLatestCandle() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);
        System.out.println("Last candle timestamp: " +
                (lastCandleTimestamp != null ? lastCandleTimestamp.format(formatter) : "null"));
        System.out.println("End date: " +
                (endDate != null ? endDate.format(formatter) : "null"));

        try {
            // ✅ Check if the last candle timestamp is beyond the subscription range
            if (lastCandleTimestamp != null && lastCandleTimestamp.isAfter(endDate)) {
                System.out.println("Last candle timestamp is after endDate, skipping modification check");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (OUT OF RANGE) <<<");
                return null;
            }

            // Find the platform stock
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock '" + stockSymbol + "' not found for platform '" + platformName + "'");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (STOCK NOT FOUND) <<<");
                return null;
            }

            PlatformStock stock = platformStockOpt.get();

            // Parse timeframe
            Timeframe timeframe;
            try {
                System.out.println("Parsing timeframe string for modification check: " + timeframeStr);
                timeframe = parseTimeframe(timeframeStr);
                System.out.println("Successfully parsed timeframe to: " + timeframe + " for modification check");
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (PARSE ERROR) <<<");
                return null;
            }

            // ✅ Get the candle at the specific timestamp (not the absolute latest)
            System.out.println("Fetching candle at timestamp: " + lastCandleTimestamp.format(formatter));
            List<MarketCandle> candles = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                            stock, timeframe, lastCandleTimestamp, lastCandleTimestamp);

            if (candles.isEmpty()) {
                System.out.println("No candle found at the specified timestamp");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (NO DATA) <<<");
                return null;
            }

            MarketCandle currentCandle = candles.get(0);
            System.out.println("Found candle at timestamp: " + currentCandle.getTimestamp().format(formatter));

            // Check if the candle has been modified
            boolean isModified = false;

            if (!currentCandle.getOpenPrice().equals(lastCandleOpen)) {
                System.out.println("Open price changed: " + lastCandleOpen + " -> " + currentCandle.getOpenPrice());
                isModified = true;
            }

            if (!currentCandle.getHighPrice().equals(lastCandleHigh)) {
                System.out.println("High price changed: " + lastCandleHigh + " -> " + currentCandle.getHighPrice());
                isModified = true;
            }

            if (!currentCandle.getLowPrice().equals(lastCandleLow)) {
                System.out.println("Low price changed: " + lastCandleLow + " -> " + currentCandle.getLowPrice());
                isModified = true;
            }

            if (!currentCandle.getClosePrice().equals(lastCandleClose)) {
                System.out.println("Close price changed: " + lastCandleClose + " -> " + currentCandle.getClosePrice());
                isModified = true;
            }

            if (!currentCandle.getVolume().equals(lastCandleVolume)) {
                System.out.println("Volume changed: " + lastCandleVolume + " -> " + currentCandle.getVolume());
                isModified = true;
            }

            if (isModified) {
                System.out.println("Candle has been modified, returning updated data");
                System.out.println(">>> RETURNING 1 MODIFIED CANDLE FOR TIMEFRAME " + timeframeStr + " <<<");
                System.out.println(">>> CANDLE TIMESTAMP: " + currentCandle.getTimestamp().format(formatter) + " <<<");
                return convertToDTO(currentCandle);
            } else {
                System.out.println("Candle has not changed");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (NO CHANGES) <<<");
                return null;
            }

        } catch (Exception e) {
            System.out.println("ERROR in getModifiedLatestCandle: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error checking for modified candle: " + e.getMessage(), e);
            System.out.println(">>> RETURNING NO MODIFIED CANDLES DUE TO ERROR FOR TIMEFRAME " + timeframeStr + " <<<");
            return null;
        }
    }

    @Override
    public CandleData getCandleAtTimestamp(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime timestamp) {

        if (timestamp == null) {
            return null;
        }

        System.out.println("\n### EXECUTING getCandleAtTimestamp() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr +
                ", timestamp=" + timestamp.format(formatter));

        try {
            // Find the platform stock
            System.out.println("Searching for platform: " + platformName + ", stock: " + stockSymbol);
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock '" + stockSymbol + "' not found for platform '" + platformName + "'");
                return null;
            }

            PlatformStock stock = platformStockOpt.get();
            System.out.println("Found matching stock: " + stock.getStockSymbol());

            // Parse timeframe
            System.out.println("Parsing timeframe: " + timeframeStr);
            Timeframe timeframe;
            try {
                timeframe = parseTimeframe(timeframeStr);
                System.out.println("Successfully parsed timeframe to: " + timeframe);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                return null;
            }

            // Get candle at specific timestamp
            System.out.println("Fetching candle at timestamp: " + timestamp.format(formatter));
            List<MarketCandle> candles = marketCandleRepository
                    .findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                            stock, timeframe, timestamp, timestamp);

            if (candles.isEmpty()) {
                System.out.println("No candle found at timestamp: " + timestamp.format(formatter));
                System.out.println(">>> RETURNING NULL FOR TIMESTAMP " + timestamp.format(formatter) + " <<<");
                return null;
            }

            CandleData result = convertToDTO(candles.get(0));
            System.out.println("Found candle at timestamp: " + timestamp.format(formatter));
            System.out.println("Candle details: O:" + result.getOpenPrice() +
                    " H:" + result.getHighPrice() +
                    " L:" + result.getLowPrice() +
                    " C:" + result.getClosePrice() +
                    " V:" + result.getVolume());
            System.out.println(">>> RETURNING CANDLE FOR TIMESTAMP " + timestamp.format(formatter) + " <<<");

            return result;

        } catch (Exception e) {
            System.out.println("ERROR in getCandleAtTimestamp: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error getting candle at timestamp: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Timeframe parseTimeframe(String timeframeStr) {
        System.out.println("Parsing timeframe string: '" + timeframeStr + "'");
        Timeframe result;
        switch (timeframeStr.toUpperCase()) {
            case "1M":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.M1 (1 minute)");
                result = Timeframe.M1;
                break;
            case "5M":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.M5 (5 minutes)");
                result = Timeframe.M5;
                break;
            case "15M":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.M15 (15 minutes)");
                result = Timeframe.M15;
                break;
            case "1H":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.H1 (1 hour)");
                result = Timeframe.H1;
                break;
            case "4H":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.H4 (4 hours)");
                result = Timeframe.H4;
                break;
            case "1D":
                System.out.println("Converting timeframe string '" + timeframeStr + "' to Timeframe.D1 (1 day)");
                result = Timeframe.D1;
                break;
            default:
                System.out.println("ERROR: Unsupported timeframe: " + timeframeStr);
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframeStr);
        }
        System.out.println("Timeframe parsed successfully to: " + result);
        return result;
    }

    private CandleData convertToDTO(MarketCandle candle) {
        CandleData dto = new CandleData();
        dto.setTimestamp(candle.getTimestamp());
        dto.setOpenPrice(candle.getOpenPrice());
        dto.setHighPrice(candle.getHighPrice());
        dto.setLowPrice(candle.getLowPrice());
        dto.setClosePrice(candle.getClosePrice());
        dto.setVolume(candle.getVolume());
        return dto;
    }
}