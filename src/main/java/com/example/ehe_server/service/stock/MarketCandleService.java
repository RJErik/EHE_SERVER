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
            //SYSTEM SET HERE
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
                System.out.println("Looking for updates for timeframe: " + timeframe + " since " +
                        lastCheckTime.format(formatter));
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                return new ArrayList<>();
            }

            // For updated candles
            System.out.println("Looking for latest candle updates since " + lastCheckTime.format(formatter));
            System.out.println("Querying database for latest candle...");

            MarketCandle latestCandle = marketCandleRepository
                    .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            stock, timeframe);

            if (latestCandle == null) {
                System.out.println("No candles found for the specified criteria");
                System.out.println(">>> RETURNING 0 CANDLES FOR TIMEFRAME " + timeframeStr + " <<<");
                return new ArrayList<>();
            }

            System.out.println("Latest candle found: " + latestCandle.getTimestamp().format(formatter));
            System.out.println("Latest candle details: O:" + latestCandle.getOpenPrice() +
                    " H:" + latestCandle.getHighPrice() +
                    " L:" + latestCandle.getLowPrice() +
                    " C:" + latestCandle.getClosePrice());

            // Check if candle is newer than last check time
            if (latestCandle.getTimestamp().isAfter(lastCheckTime)) {
                System.out.println("Latest candle is newer than last check time");
                List<CandleData> result = new ArrayList<>();
                result.add(convertToDTO(latestCandle));
                System.out.println(">>> RETURNING 1 UPDATED CANDLE FOR TIMEFRAME " + timeframeStr + " <<<");
                System.out.println(">>> CANDLE TIMESTAMP: " + latestCandle.getTimestamp().format(formatter) + " <<<");
                return result;
            }

            System.out.println("No new candles since last check time");
            System.out.println(">>> RETURNING 0 CANDLES FOR TIMEFRAME " + timeframeStr + " <<<");
            return new ArrayList<>();

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR in getUpdatedCandles: " + e.getMessage());
            e.printStackTrace();
            //SYSTEM SET HERE
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
            BigDecimal lastCandleVolume) {

        System.out.println("\n### EXECUTING getModifiedLatestCandle() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);
        System.out.println("Last candle timestamp: " +
                (lastCandleTimestamp != null ? lastCandleTimestamp.format(formatter) : "null"));

        try {
            // Skip this check for 1-minute candles since they are atomic
            if (isOneMinuteTimeframe(timeframeStr)) {
                System.out.println("Skipping modified check for 1-minute candles as they're atomic");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (ATOMIC) <<<");
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

            // Get the latest candle
            MarketCandle latestCandle = marketCandleRepository
                    .findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            stock, timeframe);

            if (latestCandle == null) {
                System.out.println("No candles found for the specified criteria");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (NO DATA) <<<");
                return null;
            }

            // If timestamps match, check if data has changed
            if (latestCandle.getTimestamp().equals(lastCandleTimestamp)) {
                boolean isModified = false;

                if (!latestCandle.getOpenPrice().equals(lastCandleOpen)) {
                    System.out.println("Open price changed: " + lastCandleOpen + " -> " + latestCandle.getOpenPrice());
                    isModified = true;
                }

                if (!latestCandle.getHighPrice().equals(lastCandleHigh)) {
                    System.out.println("High price changed: " + lastCandleHigh + " -> " + latestCandle.getHighPrice());
                    isModified = true;
                }

                if (!latestCandle.getLowPrice().equals(lastCandleLow)) {
                    System.out.println("Low price changed: " + lastCandleLow + " -> " + latestCandle.getLowPrice());
                    isModified = true;
                }

                if (!latestCandle.getClosePrice().equals(lastCandleClose)) {
                    System.out.println("Close price changed: " + lastCandleClose + " -> " + latestCandle.getClosePrice());
                    isModified = true;
                }

                if (!latestCandle.getVolume().equals(lastCandleVolume)) {
                    System.out.println("Volume changed: " + lastCandleVolume + " -> " + latestCandle.getVolume());
                    isModified = true;
                }

                if (isModified) {
                    System.out.println("Latest candle has been modified, returning updated data");
                    System.out.println(">>> RETURNING 1 MODIFIED CANDLE FOR TIMEFRAME " + timeframeStr + " <<<");
                    System.out.println(">>> CANDLE TIMESTAMP: " + latestCandle.getTimestamp().format(formatter) + " <<<");
                    return convertToDTO(latestCandle);
                } else {
                    System.out.println("Latest candle has not changed");
                    System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (NO CHANGES) <<<");
                    return null;
                }
            } else {
                // Timestamps don't match, no need to check for modifications
                // This will be caught by the regular getUpdatedCandles() method
                System.out.println("Latest candle timestamp doesn't match last known candle");
                System.out.println(">>> RETURNING NO MODIFIED CANDLES FOR TIMEFRAME " + timeframeStr + " (TIMESTAMP MISMATCH) <<<");
                return null;
            }

        } catch (Exception e) {
            System.out.println("ERROR in getModifiedLatestCandle: " + e.getMessage());
            e.printStackTrace();
            //SYSTEM SET HERE
            loggingService.logError("Error checking for modified candle: " + e.getMessage(), e);
            System.out.println(">>> RETURNING NO MODIFIED CANDLES DUE TO ERROR FOR TIMEFRAME " + timeframeStr + " <<<");
            return null;
        }
    }

    @Override
    public boolean isOneMinuteTimeframe(String timeframeStr) {
        if (timeframeStr == null) {
            return false;
        }

        return timeframeStr.equalsIgnoreCase("1m") ||
                timeframeStr.equalsIgnoreCase("1min") ||
                timeframeStr.equalsIgnoreCase("M1");
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
