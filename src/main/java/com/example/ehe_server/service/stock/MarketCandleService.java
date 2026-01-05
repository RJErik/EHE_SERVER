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
    }

    @Override
    public CandleData getLatestCandle(
            String platformName,
            String stockSymbol,
            String timeframeStr) {

        try {
            // Find the platform stock
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformName(platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStock().getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                return null;
            }

            PlatformStock stock = platformStockOpt.get();

            // Parse timeframe
            Timeframe timeframe;
            timeframe = parseTimeframe(timeframeStr);

            // Fetch the latest candle with sequence
            long startTime = System.currentTimeMillis();

            Optional<ICandleWithSequence> candleOpt = marketCandleRepository
                    .findLatestCandleWithSequence(stock.getPlatformStockId(), timeframe);

            if (candleOpt.isEmpty()) {
                return null;
            }

            ICandleWithSequence candle = candleOpt.get();

            return convertToDTO(candle);

        } catch (Exception e) {
            loggingService.logError("Error fetching latest candle: " + e.getMessage(), e);
            throw e;
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

        try {
            // Find the platform stock
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformName(platformName);

            Optional<PlatformStock> platformStockOpt = platformStocks.stream()
                    .filter(ps -> ps.getStock().getStockSymbol().equals(stockSymbol))
                    .findFirst();

            if (platformStockOpt.isEmpty()) {
                return null;
            }

            PlatformStock stock = platformStockOpt.get();

            // Parse timeframe
            Timeframe timeframe;
            timeframe = parseTimeframe(timeframeStr);

            // Get the candle at the specific timestamp WITH SEQUENCE
            Optional<ICandleWithSequence> candleOpt = marketCandleRepository
                    .findCandleWithSequenceByTimestamp(stock.getPlatformStockId(), timeframe, candleTimestamp);

            if (candleOpt.isEmpty()) {
                return null;
            }

            ICandleWithSequence candle = candleOpt.get();

            // Check if the candle has been modified
            boolean isModified = false;

            if (!candle.getOpenPrice().equals(lastOpen)) {
                isModified = true;
            }

            if (!candle.getHighPrice().equals(lastHigh)) {
                isModified = true;
            }

            if (!candle.getLowPrice().equals(lastLow)) {
                isModified = true;
            }

            if (!candle.getClosePrice().equals(lastClose)) {
                isModified = true;
            }

            if (!candle.getVolume().equals(lastVolume)) {
                isModified = true;
            }

            if (isModified) {
                return convertToDTO(candle);
            } else {
                return null;
            }

        } catch (Exception e) {
            loggingService.logError("Error checking for modified candle: " + e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public Timeframe parseTimeframe(String timeframeStr) {
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
                throw new IllegalArgumentException("Unsupported timeframe: " + timeframeStr);
        }
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
        dto.setSequence(candle.getSequence());
        return dto;
    }
}