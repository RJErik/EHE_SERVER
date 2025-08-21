package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.UnauthorizedWatchlistAccessException;
import com.example.ehe_server.exception.custom.WatchlistItemNotFoundException;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class WatchlistRemovalService implements WatchlistRemovalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final LoggingServiceInterface loggingService;

    public WatchlistRemovalService(
            WatchlistItemRepository watchlistItemRepository,
            LoggingServiceInterface loggingService) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.loggingService = loggingService;
    }

    @Override
    public void removeWatchlistItem(Integer userId, Integer watchlistItemId) {
        // Check if the watchlist item exists
        Optional<WatchlistItem> watchlistItemOptional = watchlistItemRepository.findById(watchlistItemId);
        if (watchlistItemOptional.isEmpty()) {
            throw new WatchlistItemNotFoundException(watchlistItemId);
        }

        WatchlistItem watchlistItem = watchlistItemOptional.get();

        // Verify the item belongs to the user's watchlist
        if (!watchlistItem.getWatchlist().getUser().getUserId().equals(userId)) {
            throw new UnauthorizedWatchlistAccessException(userId, watchlistItemId);
        }

        // Remove the watchlist item
        String platform = watchlistItem.getPlatformStock().getPlatformName();
        String symbol = watchlistItem.getPlatformStock().getStockSymbol();
        watchlistItemRepository.delete(watchlistItem);

        // Log success
        loggingService.logAction("Removed item from watchlist: " + platform + "/" + symbol + " (ID: " + watchlistItemId + ")");
    }
}