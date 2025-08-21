package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistCandleResponse;
import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistCandleService implements WatchlistCandleServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public WatchlistCandleService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public List<WatchlistCandleResponse> getLatestCandles(Integer userId) {
        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            return null;
        }


        // Get watchlist for the user
        Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(user);
        if (watchlistOptional.isEmpty()) {
            // No watchlist found, return empty list
            loggingService.logAction("Watchlist candles: No watchlist found for user");
            return Collections.emptyList();
        }

        Watchlist watchlist = watchlistOptional.get();
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);

        // Get latest D1 (daily) candle for each watchlist item
        // Changed from H1 to D1 timeframe
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
        loggingService.logAction("Retrieved latest daily candles for " + candles.size() + " watchlist items");

        return candles;
    }
}
