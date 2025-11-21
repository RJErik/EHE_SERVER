package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.UnauthorizedWatchlistAccessException;
import com.example.ehe_server.exception.custom.WatchlistItemNotFoundException;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class WatchlistRemovalService implements WatchlistRemovalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistRemovalService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlist.remove",
            params = {"#result.watchlistItemId",}
    )
    @Override
    public void removeWatchlistItem(Integer userId, Integer watchlistItemId) {
        // Check if the watchlist item exists
        Optional<WatchlistItem> watchlistItemOptional = watchlistItemRepository.findById(watchlistItemId);
        if (watchlistItemOptional.isEmpty()) {
            throw new WatchlistItemNotFoundException(watchlistItemId);
        }

        WatchlistItem watchlistItem = watchlistItemOptional.get();

        // Verify the item belongs to the user's watchlist
        if (!watchlistItem.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedWatchlistAccessException(userId, watchlistItemId);
        }

        // Remove the watchlist item
        watchlistItemRepository.delete(watchlistItem);
    }
}