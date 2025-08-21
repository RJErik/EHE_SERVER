package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistRetrievalResponse;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistRetrievalService implements WatchlistRetrievalServiceInterface {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public WatchlistRetrievalService(
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
    public List<WatchlistRetrievalResponse> getWatchlistItems(Integer userId) {
        // Get or create watchlist for the user
        Watchlist watchlist = watchlistRepository.findByUser(userRepository.findById(userId).orElse(null))
                .orElseGet(() -> {
                    Watchlist newWatchlist = new Watchlist();
                    newWatchlist.setUser(userRepository.findById(userId).orElse(null));
                    return watchlistRepository.save(newWatchlist);
                });

        // Get all watchlist items
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByWatchlist(watchlist);

        // Transform entities to DTOs
        List<WatchlistRetrievalResponse> items = watchlistItems.stream()
                .map(item -> new WatchlistRetrievalResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatformName(),
                        item.getPlatformStock().getStockSymbol(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Watchlist items retrieved successfully");

        return items;
    }
}