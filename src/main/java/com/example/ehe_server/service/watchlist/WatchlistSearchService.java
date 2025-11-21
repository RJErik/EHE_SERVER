package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistSearchResponse;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistSearchService implements WatchlistSearchServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistSearchService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlist.search",
            params = {
                    "#platform",
                    "#symbol",
                    "#result.size()"
            }
    )
    @Override
    public List<WatchlistSearchResponse> searchWatchlistItems(Integer userId, String platform, String symbol) {
        // Single query handles all filter combinations
        List<WatchlistItem> watchlistItems = watchlistItemRepository.searchWatchlistItems(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null
        );

        // Transform to response DTOs
        return watchlistItems.stream()
                .map(item -> new WatchlistSearchResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatformName(),
                        item.getPlatformStock().getStockSymbol(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}