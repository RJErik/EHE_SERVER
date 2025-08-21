package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistSearchResponse;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
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
    private final UserRepository userRepository;

    public WatchlistSearchService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public List<WatchlistSearchResponse> searchWatchlistItems(Integer userId, String platform, String symbol) {
        // Get watchlist for the user
        Optional<Watchlist> watchlistOptional;
        if (userRepository.existsById(userId)) {
            watchlistOptional = watchlistRepository.findByUser(userRepository.findById(userId).orElse(null));
        } else {
            return null;
        }

        if (watchlistOptional.isEmpty()) {
            // No watchlist found, return empty list
            loggingService.logAction("Watchlist search: No watchlist found for user");
            return Collections.emptyList();
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

        // Transform to response DTOs
        List<WatchlistSearchResponse> responses = watchlistItems.stream()
                .map(item -> new WatchlistSearchResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatformName(),
                        item.getPlatformStock().getStockSymbol(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Watchlist search successful, found " + responses.size() + " items");

        return responses;
    }
}
