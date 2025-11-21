package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistRetrievalResponse;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.repository.WatchlistItemRepository;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistRetrievalService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlist.get",
            params = {"#result.size()"}
    )
    @Override
    public List<WatchlistRetrievalResponse> getWatchlistItems(Integer userId) {
        // Get all watchlist items for the user directly
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

        // Transform entities to DTOs
        return watchlistItems.stream()
                .map(item -> new WatchlistRetrievalResponse(
                        item.getWatchlistItemId(),
                        item.getPlatformStock().getPlatformName(),
                        item.getPlatformStock().getStockSymbol(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}