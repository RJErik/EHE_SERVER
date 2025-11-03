package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistRetrievalResponse;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistRetrievalService implements WatchlistRetrievalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistRetrievalService(
            WatchlistItemRepository watchlistItemRepository,
            LoggingServiceInterface loggingService) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.loggingService = loggingService;
    }

    @Override
    public List<WatchlistRetrievalResponse> getWatchlistItems(Integer userId) {
        // Get all watchlist items for the user directly
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

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
        loggingService.logAction("Watchlist items retrieved successfully for user: " + userId);

        return items;
    }
}