package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
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
import java.util.List;
import java.util.Optional;

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

    /**
     * Get the latest candle for a given stock and timeframe
     */
    @Override
    public CandleData getLatestCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr) {

        System.out.println("\n### EXECUTING getLatestCandle() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);

        try {
            // Find the platform stock
            System.out.println("Querying platform stocks database for platform: " + platformName);
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformName(platformName);
            System.out.println("Found " + platformStocks.size() + " platform stocks with name: " + platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStock().getStockName().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock symbol '" + stockSymbol + "' not found for platform '" + platformName + "'");
                return null;
            }

            PlatformStock stock = platformStockOpt.get();
            System.out.println("Found matching stock: " + stock.getStock().getStockName() +
                    " (ID: " + stock.getPlatformStockId() + ")");

            // Parse timeframe
            Timeframe timeframe;
            try {
                System.out.println("Parsing timeframe string: " + timeframeStr);
                timeframe = parseTimeframe(timeframeStr);
                System.out.println("Successfully parsed timeframe to: " + timeframe);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                return null;
            }

            // Fetch the latest candle WITH SEQUENCE
            System.out.println("Fetching latest candle with sequence from database");
            long startTime = System.currentTimeMillis();

            Optional<ICandleWithSequence> candleOpt = marketCandleRepository
                    .findLatestCandleWithSequence(stock.getPlatformStockId(), timeframe);

            long queryTime = System.currentTimeMillis() - startTime;
            System.out.println("Database query completed in " + queryTime + "ms");

            if (candleOpt.isEmpty()) {
                System.out.println("WARNING: No candle data found");
                return null;
            }

            ICandleWithSequence candle = candleOpt.get();
            System.out.println("Latest candle: " +
                    candle.getTimestamp().format(formatter) + " | Seq:" + candle.getSequence() +
                    " O:" + candle.getOpenPrice() + " H:" + candle.getHighPrice() +
                    " L:" + candle.getLowPrice() + " C:" + candle.getClosePrice() +
                    " V:" + candle.getVolume());

            CandleData result = convertToDTO(candle);
            System.out.println(">>> RETURNING LATEST CANDLE FOR " + stockSymbol + " (" + timeframeStr +
                    ") WITH SEQUENCE " + result.getSequence() + " <<<");
            return result;

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR in getLatestCandle: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error fetching latest candle: " + e.getMessage(), e);
            return null;
        }
    }


    /**
     * Check if the candle at a specific timestamp has been modified
     * Returns the updated candle if modified, null otherwise
     */
    @Override
    public CandleData getModifiedCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr,
            LocalDateTime candleTimestamp,
            BigDecimal lastOpen,
            BigDecimal lastHigh,
            BigDecimal lastLow,
            BigDecimal lastClose,
            BigDecimal lastVolume) {

        System.out.println("\n### EXECUTING getModifiedCandle() ###");
        System.out.println("Request params: platform=" + platformName +
                ", stock=" + stockSymbol +
                ", timeframe=" + timeframeStr);
        System.out.println("Checking candle at timestamp: " +
                (candleTimestamp != null ? candleTimestamp.format(formatter) : "null"));

        try {
            // Find the platform stock
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformName(platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStock().getStockName().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                System.out.println("ERROR: Stock '" + stockSymbol + "' not found for platform '" + platformName + "'");
                return null;
            }

            PlatformStock stock = platformStockOpt.get();

            // Parse timeframe
            Timeframe timeframe;
            try {
                timeframe = parseTimeframe(timeframeStr);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Failed to parse timeframe '" + timeframeStr + "': " + e.getMessage());
                return null;
            }

            // Get the candle at the specific timestamp WITH SEQUENCE
            System.out.println("Fetching candle with sequence at timestamp: " + candleTimestamp.format(formatter));
            Optional<ICandleWithSequence> candleOpt = marketCandleRepository
                    .findCandleWithSequenceByTimestamp(stock.getPlatformStockId(), timeframe, candleTimestamp);

            if (candleOpt.isEmpty()) {
                System.out.println("No candle found at the specified timestamp");
                return null;
            }

            ICandleWithSequence candle = candleOpt.get();
            System.out.println("Found candle at timestamp: " + candle.getTimestamp().format(formatter) +
                    " with sequence: " + candle.getSequence());

            // Check if the candle has been modified
            boolean isModified = false;

            if (!candle.getOpenPrice().equals(lastOpen)) {
                System.out.println("Open price changed: " + lastOpen + " -> " + candle.getOpenPrice());
                isModified = true;
            }

            if (!candle.getHighPrice().equals(lastHigh)) {
                System.out.println("High price changed: " + lastHigh + " -> " + candle.getHighPrice());
                isModified = true;
            }

            if (!candle.getLowPrice().equals(lastLow)) {
                System.out.println("Low price changed: " + lastLow + " -> " + candle.getLowPrice());
                isModified = true;
            }

            if (!candle.getClosePrice().equals(lastClose)) {
                System.out.println("Close price changed: " + lastClose + " -> " + candle.getClosePrice());
                isModified = true;
            }

            if (!candle.getVolume().equals(lastVolume)) {
                System.out.println("Volume changed: " + lastVolume + " -> " + candle.getVolume());
                isModified = true;
            }

            if (isModified) {
                System.out.println(">>> CANDLE WAS MODIFIED, RETURNING UPDATED DATA WITH SEQUENCE " +
                        candle.getSequence() + " <<<");
                return convertToDTO(candle);
            } else {
                System.out.println(">>> CANDLE HAS NOT CHANGED <<<");
                return null;
            }

        } catch (Exception e) {
            System.out.println("ERROR in getModifiedCandle: " + e.getMessage());
            e.printStackTrace();
            loggingService.logError("Error checking for modified candle: " + e.getMessage(), e);
            return null;
        }
    }


    @Override
    public Timeframe parseTimeframe(String timeframeStr) {
        System.out.println("Parsing timeframe string: '" + timeframeStr + "'");
        Timeframe result;
        switch (timeframeStr.toUpperCase()) {
            case "1M":
                result = Timeframe.M1;
                break;
            case "5M":
                result = Timeframe.M5;
                break;
            case "15M":
                result = Timeframe.M15;
                break;
            case "1H":
                result = Timeframe.H1;
                break;
            case "4H":
                result = Timeframe.H4;
                break;
            case "1D":
                result = Timeframe.D1;
                break;
            default:
                System.out.println("ERROR: Unsupported timeframe: " + timeframeStr);
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframeStr);
        }
        System.out.println("Timeframe parsed successfully to: " + result);
        return result;
    }

    private CandleData convertToDTO(ICandleWithSequence candle) {
        CandleData dto = new CandleData();
        dto.setTimestamp(candle.getTimestamp());
        dto.setOpenPrice(candle.getOpenPrice());
        dto.setHighPrice(candle.getHighPrice());
        dto.setLowPrice(candle.getLowPrice());
        dto.setClosePrice(candle.getClosePrice());
        dto.setVolume(candle.getVolume());
        dto.setSequence(candle.getSequence()); // Set the sequence number
        return dto;
    }
}