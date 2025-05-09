package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WatchlistSearchService implements WatchlistSearchServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistSearchService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> searchWatchlistItems(String platform, String symbol) {
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
                loggingService.logAction(null, userIdStr, "Watchlist search failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Watchlist search failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get watchlist for the user
            Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(user);
            if (watchlistOptional.isEmpty()) {
                // No watchlist found, return empty list
                result.put("success", true);
                result.put("items", Collections.emptyList());
                loggingService.logAction(userId, userIdStr, "Watchlist search: No watchlist found for user");
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
                loggingService.logAction(userId, userIdStr,
                        "Searching watchlist with platform=" + platform + " and symbol=" + symbol);
            } else if (platform != null && !platform.trim().isEmpty()) {
                // Search by platform only
                watchlistItems = watchlistItemRepository
                        .findByWatchlistAndPlatformStock_PlatformName(watchlist, platform);
                loggingService.logAction(userId, userIdStr, "Searching watchlist with platform=" + platform);
            } else if (symbol != null && !symbol.trim().isEmpty()) {
                // Search by symbol only
                watchlistItems = watchlistItemRepository
                        .findByWatchlistAndPlatformStock_StockSymbol(watchlist, symbol);
                loggingService.logAction(userId, userIdStr, "Searching watchlist with symbol=" + symbol);
            } else {
                // No filters, get all items
                watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);
                loggingService.logAction(userId, userIdStr, "Searching watchlist with no filters");
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
            loggingService.logAction(userId, userIdStr,
                    "Watchlist search successful, found " + items.size() + " items");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error searching watchlist items: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching watchlist items");
        }

        return result;
    }
}
