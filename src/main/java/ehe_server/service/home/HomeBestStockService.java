package ehe_server.service.home;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.HomeStockResponse;
import ehe_server.entity.MarketCandle;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.service.intf.home.HomeBestStockServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
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
                        marketCandle.getPlatformStock().getStock().getStockSymbol(),
                        marketCandle.getOpenPrice().divide(marketCandle.getClosePrice(), 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }
}