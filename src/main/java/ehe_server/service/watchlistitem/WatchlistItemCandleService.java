package ehe_server.service.watchlistitem;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.WatchlistCandleResponse;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.PlatformStock;
import ehe_server.entity.WatchlistItem;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.repository.WatchlistItemRepository;
import ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WatchlistItemCandleService implements WatchlistCandleServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final MarketCandleRepository marketCandleRepository;

    public WatchlistItemCandleService(
            WatchlistItemRepository watchlistItemRepository,
            MarketCandleRepository marketCandleRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketCandleRepository = marketCandleRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.candles.get",
            params = {"#result.size()"}
    )
    @Override
    public List<WatchlistCandleResponse> getLatestCandles(Integer userId) {

        // Data retrieval
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

        if (watchlistItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<PlatformStock> stocks = watchlistItems.stream()
                .map(WatchlistItem::getPlatformStock)
                .collect(Collectors.toList());

        // Batch fetch candles
        List<MarketCandle> candles = marketCandleRepository.findLatestCandlesForStocks(
                stocks,
                MarketCandle.Timeframe.D1
        );

        // Data processing and response mapping
        Map<Integer, MarketCandle> candleMap = candles.stream()
                .collect(Collectors.toMap(
                        c -> c.getPlatformStock().getPlatformStockId(),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        return watchlistItems.stream()
                .map(item -> {
                    MarketCandle candle = candleMap.get(item.getPlatformStock().getPlatformStockId());

                    if (candle != null) {
                        return new WatchlistCandleResponse(
                                item.getWatchlistItemId(),
                                item.getPlatformStock().getPlatform().getPlatformName(),
                                item.getPlatformStock().getStock().getStockSymbol(),
                                candle.getTimestamp(),
                                candle.getTimeframe(),
                                candle.getOpenPrice(),
                                candle.getHighPrice(),
                                candle.getLowPrice(),
                                candle.getClosePrice(),
                                candle.getVolume()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}