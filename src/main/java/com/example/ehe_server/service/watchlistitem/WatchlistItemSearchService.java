package com.example.ehe_server.service.watchlistitem;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistResponse;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WatchlistItemSearchService implements WatchlistSearchServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistItemSearchService(WatchlistItemRepository watchlistItemRepository,
                                      UserRepository userRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
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

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

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
                        item.getPlatformStock().getStock().getStockName(),
                        item.getDateAdded().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}