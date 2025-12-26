package com.example.ehe_server.service.watchlistitem;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistCandleResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistItemCandleService(
            WatchlistItemRepository watchlistItemRepository,
            MarketCandleRepository marketCandleRepository,
            UserRepository userRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.candles.get",
            params = {"#result.size()"}
    )
    @Override
    public List<WatchlistCandleResponse> getLatestCandles(Integer userId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

        if (watchlistItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<PlatformStock> stocks = watchlistItems.stream()
                .map(WatchlistItem::getPlatformStock)
                .collect(Collectors.toList());

        // Batch fetch candles to avoid N+1 performance issues
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
                                item.getPlatformStock().getStock().getStockName(),
                                candle.getTimestamp().format(DATE_FORMATTER),
                                candle.getTimeframe().getValue(),
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