package ehe_server.service.home;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.HomeStockResponse;
import ehe_server.entity.MarketCandle;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.service.intf.home.HomeWorstStockServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HomeWorstStockService implements HomeWorstStockServiceInterface {
    private final MarketCandleRepository marketCandleRepository;

    public HomeWorstStockService(MarketCandleRepository marketCandleRepository) {
        this.marketCandleRepository = marketCandleRepository;
    }

    @LogMessage(messageKey = "log.message.home.worstStock")
    @Override
    public List<HomeStockResponse> getHomeWorstStock() {
        List<MarketCandle> marketCandles = marketCandleRepository.findBottomTenDailyCandlesByPercentageChange();

        return marketCandles.stream()
                .map(marketCandle -> {
                    BigDecimal percentageChange = marketCandle.getClosePrice()
                            .subtract(marketCandle.getOpenPrice())
                            .divide(marketCandle.getOpenPrice(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);

                    return new HomeStockResponse(
                            marketCandle.getPlatformStock().getPlatform().getPlatformName(),
                            marketCandle.getPlatformStock().getStock().getStockSymbol(),
                            percentageChange
                    );
                })
                .toList();
    }
}