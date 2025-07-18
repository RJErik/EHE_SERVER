package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistService implements WatchlistServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public WatchlistService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getWatchlistItems() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get or create watchlist for the user
            Watchlist watchlist = watchlistRepository.findByUser(userContextService.getCurrentHumanUser())
                    .orElseGet(() -> {
                        Watchlist newWatchlist = new Watchlist();
                        newWatchlist.setUser(userContextService.getCurrentHumanUser());
                        return watchlistRepository.save(newWatchlist);
                    });

            // Get all watchlist items
            List<WatchlistItem> watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);

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
            loggingService.logAction("Watchlist items retrieved successfully");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving watchlist items: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving watchlist items");
        }

        return result;
    }

    @Override
    public Map<String, Object> addWatchlistItem(String platform, String symbol) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if platform and symbol combination exists
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
            if (platformStocks.isEmpty()) {
                result.put("success", false);
                result.put("message", "Platform and symbol combination does not exist");
                loggingService.logAction("Watchlist add failed: Platform/symbol combination not found: " + platform + "/" + symbol);
                return result;
            }

            PlatformStock platformStock = platformStocks.get(0);

            // Get or create watchlist for the user
            Watchlist watchlist = watchlistRepository.findByUser(userContextService.getCurrentHumanUser())
                    .orElseGet(() -> {
                        Watchlist newWatchlist = new Watchlist();
                        newWatchlist.setUser(userContextService.getCurrentHumanUser());
                        return watchlistRepository.save(newWatchlist);
                    });

            // Check if item already exists in watchlist
            Optional<WatchlistItem> existingItem = watchlistItemRepository.findByWatchlistAndPlatformStock(watchlist, platformStock);
            if (existingItem.isPresent()) {
                result.put("success", false);
                result.put("message", "Item already exists in watchlist");
                loggingService.logAction("Watchlist add failed: Item already exists: " + platform + "/" + symbol);
                return result;
            }

            // Create new watchlist item
            WatchlistItem newItem = new WatchlistItem();
            newItem.setWatchlist(watchlist);
            newItem.setPlatformStock(platformStock);
            newItem.setDateAdded(LocalDateTime.now());
            WatchlistItem savedItem = watchlistItemRepository.save(newItem);

            // Prepare success response
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", savedItem.getWatchlistItemId());
            itemMap.put("platform", platform);
            itemMap.put("symbol", symbol);
            itemMap.put("dateAdded", savedItem.getDateAdded().format(DATE_FORMATTER));

            result.put("success", true);
            result.put("message", "Item added to watchlist successfully");
            result.put("item", itemMap);

            // Log success
            loggingService.logAction("Added item to watchlist: " + platform + "/" + symbol);

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error adding watchlist item: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while adding item to watchlist");
        }

        return result;
    }

    @Override
    public Map<String, Object> removeWatchlistItem(Integer watchlistItemId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            String userIdStr = userContextService.getCurrentUserIdAsString();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if the watchlist item exists
            Optional<WatchlistItem> watchlistItemOptional = watchlistItemRepository.findById(watchlistItemId);
            if (watchlistItemOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Watchlist item not found");
                loggingService.logAction("Watchlist remove failed: Item not found with ID: " + watchlistItemId);
                return result;
            }

            WatchlistItem watchlistItem = watchlistItemOptional.get();

            // Verify the item belongs to the user's watchlist
            if (!watchlistItem.getWatchlist().getUser().getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "Not authorized to remove this watchlist item");
                loggingService.logAction("Watchlist remove failed: Unauthorized access to item ID: " + watchlistItemId);
                return result;
            }

            // Remove the watchlist item
            String platform = watchlistItem.getPlatformStock().getPlatformName();
            String symbol = watchlistItem.getPlatformStock().getStockSymbol();
            watchlistItemRepository.delete(watchlistItem);

            // Prepare success response
            result.put("success", true);
            result.put("message", "Item removed from watchlist successfully");

            // Log success
            loggingService.logAction("Removed item from watchlist: " + platform + "/" + symbol + " (ID: " + watchlistItemId + ")");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error removing watchlist item: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while removing item from watchlist");
        }

        return result;
    }
}
