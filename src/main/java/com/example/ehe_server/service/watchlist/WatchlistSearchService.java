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
        if (!userRepository.existsById(userId)) {
            loggingService.logAction("Watchlist search: User not found");
            return Collections.emptyList();
        }

        Optional<Watchlist> watchlistOptional = watchlistRepository.findByUser(
                userRepository.findById(userId).orElse(null)
        );

        if (watchlistOptional.isEmpty()) {
            loggingService.logAction("Watchlist search: No watchlist found for user");
            return Collections.emptyList();
        }

        Watchlist watchlist = watchlistOptional.get();

        // Single query handles all filter combinations
        List<WatchlistItem> watchlistItems = watchlistItemRepository.searchWatchlistItems(
                watchlist,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null
        );

        loggingService.logAction("Searching watchlist with platform=" + platform + " and symbol=" + symbol);

        // Transform to response DTOs
        List<WatchlistSearchResponse> responses = watchlistItems.stream()
                .map(item -> new WatchlistSearchResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatformName(),
                        item.getPlatformStock().getStockSymbol(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

        loggingService.logAction("Watchlist search successful, found " + responses.size() + " items");

        return responses;
    }
}
