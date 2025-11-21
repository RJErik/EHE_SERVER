package com.example.ehe_server.service.home;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.HomeWorstStockResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.service.intf.home.HomeWorstStockServiceInterface;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service
public class HomeWorstStockService implements HomeWorstStockServiceInterface {
    private final MarketCandleRepository marketCandleRepository;

    public HomeWorstStockService(MarketCandleRepository marketCandleRepository) {
        this.marketCandleRepository = marketCandleRepository;
    }

    @LogMessage(messageKey = "log.message.home.worstStock")
    @Override
    public List<HomeWorstStockResponse> getHomeWorstStock() {
        List<MarketCandle> marketCandles = marketCandleRepository.findBottomTenDailyCandlesByPercentageChange();

        return marketCandles.stream()
                .map(marketCandle -> new HomeWorstStockResponse(
                        marketCandle.getPlatformStock().getPlatformName(),
                        marketCandle.getPlatformStock().getStockSymbol(),
                        marketCandle.getOpenPrice().divide(marketCandle.getClosePrice(), 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }
}
