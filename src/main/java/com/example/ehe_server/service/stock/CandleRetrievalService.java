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
    public CandlesResponse getCandlesBySequence(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
                                                Long fromSequence, Long toSequence) {
        // Find the platform stock
        PlatformStock platformStock = findPlatformStock(platform, stockSymbol);

        // Retrieve candles by sequence range (Now returns Projection)
        List<CandleWithSequenceInterface> candles = marketCandleRepository.findByStockAndTimeframeAndSequenceRange(
                platformStock.getPlatformStockId(),
                timeframe,
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
                timeframe.toString(),
                candleDTOs.size(),
                candleDTOs
        );
    }

    @LogMessage(
            messageKey = "log.message.stock.candles.date.get",
            params = {"#platform", "#stockSymbol", "#timeframe", "#fromDate", "#toDate", "#result.totalCandles"}
    )
    @Override
    public CandlesResponse getCandlesByDate(String platform, String stockSymbol, MarketCandle.Timeframe timeframe,
                                            LocalDateTime fromDate, LocalDateTime toDate) {
        // Find the platform stock
        PlatformStock platformStock = findPlatformStock(platform, stockSymbol);

        // Retrieve candles by date range (Using new method to get sequence numbers)
        List<CandleWithSequenceInterface> candles = marketCandleRepository
                .findCandlesByDateRangeWithSequence(
                        platformStock.getPlatformStockId(),
                        timeframe,
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
                timeframe.toString(),
                candleDTOs.size(),
                candleDTOs
        );
    }

    private PlatformStock findPlatformStock(String platform, String stockSymbol) {
        List<PlatformStock> platformStocks = platformStockRepository
                .findByPlatformPlatformNameAndStockStockSymbol(platform, stockSymbol);

        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, stockSymbol);
        }

        return platformStocks.getFirst();
    }

    private CandleDTO convertToDTO(CandleWithSequenceInterface candle) {
        return new CandleDTO(
                candle.getMarketCandleId(),
                candle.getTimestamp(),
                candle.getOpenPrice(),
                candle.getClosePrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getVolume(),
                candle.getSequence()
        );
    }
}