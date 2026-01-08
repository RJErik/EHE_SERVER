package ehe_server.service.watchlistitem;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.UnauthorizedWatchlistAccessException;
import ehe_server.exception.custom.WatchlistItemNotFoundException;
import ehe_server.repository.WatchlistItemRepository;
import ehe_server.service.intf.watchlist.WatchlistRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WatchlistItemRemovalService implements WatchlistRemovalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistItemRemovalService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.remove",
            params = {"#watchlistItemId"}
    )
    @Override
    public void removeWatchlistItem(Integer userId, Integer watchlistItemId) {

        // Existence check
        WatchlistItem watchlistItem = watchlistItemRepository.findById(watchlistItemId)
                .orElseThrow(() -> new WatchlistItemNotFoundException(watchlistItemId));

        // Authorization verification
        if (!watchlistItem.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedWatchlistAccessException(userId, watchlistItemId);
        }

        // Execute removal
        watchlistItemRepository.delete(watchlistItem);
    }
}