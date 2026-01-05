package com.example.ehe_server.service.watchlistitem;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistResponse;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WatchlistItemSearchService implements WatchlistSearchServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistItemSearchService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.search",
            params = {
                    "#platform",
                    "#symbol",
                    "#result.size()"
            }
    )
    @Override
    public List<WatchlistResponse> searchWatchlistItems(Integer userId, String platform, String symbol) {

        // Data retrieval
        List<WatchlistItem> watchlistItems = watchlistItemRepository.searchWatchlistItems(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null
        );

        // Response mapping
        return watchlistItems.stream()
                .map(item -> new WatchlistResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatform().getPlatformName(),
                        item.getPlatformStock().getStock().getStockSymbol(),
                        item.getDateAdded()
                ))
                .collect(Collectors.toList());
    }
}