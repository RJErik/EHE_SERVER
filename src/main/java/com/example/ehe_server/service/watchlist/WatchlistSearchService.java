package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistSearchService implements WatchlistSearchServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public WatchlistSearchService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> searchWatchlistItems(String platform, String symbol) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get watchlist for the user
            Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(userContextService.getCurrentHumanUser());
            if (watchlistOptional.isEmpty()) {
                // No watchlist found, return empty list
                result.put("success", true);
                result.put("items", Collections.emptyList());
                loggingService.logAction("Watchlist search: No watchlist found for user");
                return result;
            }

            Watchlist watchlist = watchlistOptional.get();
            List<WatchlistItem> watchlistItems;

            // Apply search filters
            if (platform != null && !platform.trim().isEmpty() && symbol != null && !symbol.trim().isEmpty()) {
                // Search by both platform and symbol
                watchlistItems = watchlistItemRepository
                        .findByWatchlistAndPlatformStock_PlatformNameAndPlatformStock_StockSymbol(
                                watchlist, platform, symbol);
                loggingService.logAction("Searching watchlist with platform=" + platform + " and symbol=" + symbol);
            } else if (platform != null && !platform.trim().isEmpty()) {
                // Search by platform only
                watchlistItems = watchlistItemRepository
                        .findByWatchlistAndPlatformStock_PlatformName(watchlist, platform);
                loggingService.logAction("Searching watchlist with platform=" + platform);
            } else if (symbol != null && !symbol.trim().isEmpty()) {
                // Search by symbol only
                watchlistItems = watchlistItemRepository
                        .findByWatchlistAndPlatformStock_StockSymbol(watchlist, symbol);
                loggingService.logAction("Searching watchlist with symbol=" + symbol);
            } else {
                // No filters, get all items
                watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);
                loggingService.logAction("Searching watchlist with no filters");
            }

            // Transform to response format
            List<Map<String, Object>> items = watchlistItems.stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", item.getWatchlistItemId());
                        itemMap.put("platform", item.getPlatformStock().getPlatformName());
                        itemMap.put("symbol", item.getPlatformStock().getStockSymbol());
                        itemMap.put("dateAdded", item.getDateAdded().format(DATE_FORMATTER));
                        return itemMap;
                    })
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("items", items);

            // Log success
            loggingService.logAction("Watchlist search successful, found " + items.size() + " items");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error searching watchlist items: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching watchlist items");
        }

        return result;
    }
}
