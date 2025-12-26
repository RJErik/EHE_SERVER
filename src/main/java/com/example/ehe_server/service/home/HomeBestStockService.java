package com.example.ehe_server.service.home;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.HomeStockResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.service.intf.home.HomeBestStockServiceInterface;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service
public class HomeBestStockService implements HomeBestStockServiceInterface {
    private final MarketCandleRepository marketCandleRepository;

    public HomeBestStockService(MarketCandleRepository marketCandleRepository) {
        this.marketCandleRepository = marketCandleRepository;
    }

    @LogMessage(messageKey = "log.message.home.bestStock")
    @Override
    public List<HomeStockResponse> getHomeBestStock() {
        List<MarketCandle> marketCandles = marketCandleRepository.findTopTenDailyCandlesByPercentageChange();

        return marketCandles.stream()
                .map(marketCandle -> new HomeStockResponse(
                        marketCandle.getPlatformStock().getPlatform().getPlatformName(),
                        marketCandle.getPlatformStock().getStock().getStockName(),
                        marketCandle.getOpenPrice().divide(marketCandle.getClosePrice(), 2, RoundingMode.HALF_UP)
                ))
                .toList();
}}
