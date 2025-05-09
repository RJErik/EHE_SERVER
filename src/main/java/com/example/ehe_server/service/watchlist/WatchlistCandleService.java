package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.MarketCandleRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCandleServiceInterface;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WatchlistCandleService implements WatchlistCandleServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistCandleService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            UserRepository userRepository,
            MarketCandleRepository marketCandleRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> getLatestCandles() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Watchlist candles failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Watchlist candles failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get watchlist for the user
            Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(user);
            if (watchlistOptional.isEmpty()) {
                // No watchlist found, return empty list
                result.put("success", true);
                result.put("candles", Collections.emptyList());
                loggingService.logAction(userId, userIdStr, "Watchlist candles: No watchlist found for user");
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
            loggingService.logAction(userId, userIdStr,
                    "Retrieved latest daily candles for " + candles.size() + " watchlist items");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving watchlist candles: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving watchlist candles");
        }

        return result;
    }
}
