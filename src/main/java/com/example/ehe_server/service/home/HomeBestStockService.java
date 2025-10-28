package com.example.ehe_server.service.home;

import com.example.ehe_server.dto.HomeBestStockResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.service.intf.home.HomeBestStockServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service
public class HomeBestStockService implements HomeBestStockServiceInterface {
    private final LoggingServiceInterface loggingService;
    private final MarketCandleRepository marketCandleRepository;

    public HomeBestStockService(MarketCandleRepository marketCandleRepository,
                                LoggingServiceInterface loggingService) {
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
    }
    @Override
    public List<HomeBestStockResponse> getHomeBestStock() {
        List<MarketCandle> marketCandles = marketCandleRepository.findTopTenDailyCandlesByPercentageChange();

        List<HomeBestStockResponse> homeBestStockResponses = marketCandles.stream()
                .map(marketCandle -> new HomeBestStockResponse(
                        marketCandle.getPlatformStock().getPlatformName(),
                        marketCandle.getPlatformStock().getStockSymbol(),
                        marketCandle.getOpenPrice().divide(marketCandle.getClosePrice(), 2, RoundingMode.HALF_UP)
                ))
                .toList();

        loggingService.logAction("Retrieved the daily best stocks.");
        return homeBestStockResponses;
}}
