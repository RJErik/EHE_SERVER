package com.example.ehe_server.service.home;

import com.example.ehe_server.dto.HomeWorstStockResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.service.intf.home.HomeWorstStockServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service
public class HomeWorstStockService implements HomeWorstStockServiceInterface {
    private final LoggingServiceInterface loggingService;
    private final MarketCandleRepository marketCandleRepository;

    public HomeWorstStockService(MarketCandleRepository marketCandleRepository,
                                 LoggingServiceInterface loggingService) {
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
    }
    @Override
    public List<HomeWorstStockResponse> getHomeWorstStock() {
        List<MarketCandle> marketCandles = marketCandleRepository.findBottomTenDailyCandlesByPercentageChange();

        List<HomeWorstStockResponse> homeWorstStockResponses = marketCandles.stream()
                .map(marketCandle -> new HomeWorstStockResponse(
                        marketCandle.getPlatformStock().getPlatformName(),
                        marketCandle.getPlatformStock().getStockSymbol(),
                        marketCandle.getOpenPrice().divide(marketCandle.getClosePrice(), 2, RoundingMode.HALF_UP)
                ))
                .toList();

        loggingService.logAction("Retrieved the daily worst stocks.");
        return homeWorstStockResponses;
    }
}
