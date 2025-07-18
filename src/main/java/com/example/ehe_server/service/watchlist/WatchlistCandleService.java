package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class WatchlistCandleService implements WatchlistCandleServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public WatchlistCandleService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getLatestCandles() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();


            // Get watchlist for the user
            Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(user);
            if (watchlistOptional.isEmpty()) {
                // No watchlist found, return empty list
                result.put("success", true);
                result.put("candles", Collections.emptyList());
                loggingService.logAction("Watchlist candles: No watchlist found for user");
                return result;
            }

            Watchlist watchlist = watchlistOptional.get();
            List<WatchlistItem> watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);

            // Get latest D1 (daily) candle for each watchlist item
            // Changed from H1 to D1 timeframe
            List<Map<String, Object>> candles = new ArrayList<>();
            for (WatchlistItem item : watchlistItems) {
                MarketCandle candle = marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        item.getPlatformStock(), MarketCandle.Timeframe.D1);

                if (candle != null) {
                    Map<String, Object> candleMap = new HashMap<>();
                    candleMap.put("watchlistItemId", item.getWatchlistItemId());
                    candleMap.put("platform", item.getPlatformStock().getPlatformName());
                    candleMap.put("symbol", item.getPlatformStock().getStockSymbol());
                    candleMap.put("timestamp", candle.getTimestamp().format(DATE_FORMATTER));
                    candleMap.put("timeframe", candle.getTimeframe().getValue());
                    candleMap.put("open", candle.getOpenPrice());
                    candleMap.put("high", candle.getHighPrice());
                    candleMap.put("low", candle.getLowPrice());
                    candleMap.put("close", candle.getClosePrice());
                    candleMap.put("volume", candle.getVolume());
                    candles.add(candleMap);
                }
            }

            // Prepare success response
            result.put("success", true);
            result.put("candles", candles);

            // Log success
            loggingService.logAction("Retrieved latest daily candles for " + candles.size() + " watchlist items");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving watchlist candles: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving watchlist candles");
        }

        return result;
    }
}
