package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistCandleResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistCandleService implements WatchlistCandleServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistCandleService(
            WatchlistItemRepository watchlistItemRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
    }

    @Override
    public List<WatchlistCandleResponse> getLatestCandles(Integer userId) {
        // Get all watchlist items for the user directly
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

        if (watchlistItems.isEmpty()) {
            //TODO throw error
            return Collections.emptyList();
        }

        // Get latest D1 (daily) candle for each watchlist item and map to DTO
        List<WatchlistCandleResponse> candles = watchlistItems.stream()
                .map(item -> {
                    MarketCandle candle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                            item.getPlatformStock(), MarketCandle.Timeframe.D1);
                    if (candle != null) {
                        return new WatchlistCandleResponse(
                                item.getWatchlistItemId(),
                                item.getPlatformStock().getPlatformName(),
                                item.getPlatformStock().getStockSymbol(),
                                candle.getTimestamp().format(DATE_FORMATTER),
                                candle.getTimeframe().getValue(),
                                candle.getOpenPrice(),
                                candle.getHighPrice(),
                                candle.getLowPrice(),
                                candle.getClosePrice(),
                                candle.getVolume()
                        );
                    }
                    return null; // Handle items without a candle
                })
                .filter(Objects::nonNull) // Filter out items with no candle data
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Retrieved latest daily candles for user: " + userId + ", found " + candles.size() + " items with candle data");

        return candles;
    }
}