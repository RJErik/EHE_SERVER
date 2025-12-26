package com.example.ehe_server.service.watchlistitem;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WatchlistItemRemovalService implements WatchlistRemovalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;

    public WatchlistItemRemovalService(WatchlistItemRepository watchlistItemRepository,
                                       UserRepository userRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;

    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.remove",
            params = {"#watchlistItemId"}
    )
    @Override
    public void removeWatchlistItem(Integer userId, Integer watchlistItemId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (watchlistItemId == null) {
            throw new MissingWatchlistItemIdException();
        }

        // Existence check
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

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