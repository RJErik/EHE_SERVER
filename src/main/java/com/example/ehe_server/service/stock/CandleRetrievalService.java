package com.example.ehe_server.service.stock;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.CandleDTO;
import com.example.ehe_server.dto.CandlesResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.stock.CandleRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CandleRetrievalService implements CandleRetrievalServiceInterface {

    private final MarketCandleRepository marketCandleRepository;
    private final PlatformStockRepository platformStockRepository;

    public CandleRetrievalService(MarketCandleRepository marketCandleRepository,
                                  PlatformStockRepository platformStockRepository) {
        this.marketCandleRepository = marketCandleRepository;
        this.platformStockRepository = platformStockRepository;
    }

    @LogMessage(
            messageKey = "log.message.stock.candles.sequence.get",
            params = {"#platform", "#stockSymbol", "#timeframe", "#fromSequence", "#toSequence", "#result.totalCandles"}
    )
    @Override
    public CandlesResponse getCandlesBySequence(String platform, String stockSymbol, String timeframe,
                                                Long fromSequence, Long toSequence) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }

        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new MissingTimeframeException();
        }

        if (fromSequence == null || toSequence == null) {
            throw new MissingSequenceNumberException();
        }

        // Find the platform stock
        PlatformStock platformStock = findPlatformStock(platform, stockSymbol);

        // Parse and validate timeframe
        MarketCandle.Timeframe tf = parseTimeframe(timeframe);

        // Retrieve candles by sequence range (Now returns Projection)
        List<ICandleWithSequence> candles = marketCandleRepository.findByStockAndTimeframeAndSequenceRange(
                platformStock.getPlatformStockId(),
                tf,
                fromSequence,
                toSequence
        );

        // Convert to DTOs
        List<CandleDTO> candleDTOs = candles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Build response
        return new CandlesResponse(
                platform,
                stockSymbol,
                timeframe,
                candleDTOs.size(),
                candleDTOs
        );
    }

    @LogMessage(
            messageKey = "log.message.stock.candles.date.get",
            params = {"#platform", "#stockSymbol", "#timeframe", "#fromDate", "#toDate", "#result.totalCandles"}
    )
    @Override
    public CandlesResponse getCandlesByDate(String platform, String stockSymbol, String timeframe,
                                            LocalDateTime fromDate, LocalDateTime toDate) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }

        if (timeframe == null || timeframe.trim().isEmpty()) {
            throw new MissingTimeframeException();
        }

        if (fromDate == null || toDate == null) {
            throw new MissingDateRangeException();
        }

        // Find the platform stock
        PlatformStock platformStock = findPlatformStock(platform, stockSymbol);

        // Parse and validate timeframe
        MarketCandle.Timeframe tf = parseTimeframe(timeframe);

        // Retrieve candles by date range (Using new method to get sequence numbers)
        List<ICandleWithSequence> candles = marketCandleRepository
                .findCandlesByDateRangeWithSequence(
                        platformStock.getPlatformStockId(),
                        tf,
                        fromDate,
                        toDate
                );

        // Convert to DTOs
        List<CandleDTO> candleDTOs = candles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Build response
        return new CandlesResponse(
                platform,
                stockSymbol,
                timeframe,
                candleDTOs.size(),
                candleDTOs
        );
    }

    private PlatformStock findPlatformStock(String platform, String stockSymbol) {
        List<PlatformStock> platformStocks = platformStockRepository
                .findByPlatformPlatformNameAndStockStockName(platform, stockSymbol);

        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, stockSymbol);
        }

        return platformStocks.getFirst();
    }


    private MarketCandle.Timeframe parseTimeframe(String timeframe) {
        try {
            return MarketCandle.Timeframe.fromValue(timeframe);
        } catch (IllegalArgumentException e) {
            throw new InvalidTimeframeException(timeframe);
        }
    }

    /**
     * Converts a Projection Interface to CandleDTO
     *
     * @param candle The ICandleWithSequence projection
     * @return CandleDTO with sequence
     */
    private CandleDTO convertToDTO(ICandleWithSequence candle) {
        return new CandleDTO(
                candle.getMarketCandleId(),
                candle.getTimestamp(),
                candle.getOpenPrice(),
                candle.getClosePrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getVolume(),
                candle.getSequence() // Map the sequence number
        );
    }
}