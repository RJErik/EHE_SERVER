package ehe_server.service.watchlistitem;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.WatchlistResponse;
import ehe_server.entity.WatchlistItem;
import ehe_server.repository.WatchlistItemRepository;
import ehe_server.service.intf.watchlist.WatchlistRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WatchlistItemRetrievalService implements WatchlistRetrievalServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;

    public WatchlistItemRetrievalService(WatchlistItemRepository watchlistItemRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.get",
            params = {"#result.size()"}
    )
    @Override
    public List<WatchlistResponse> getWatchlistItems(Integer userId) {

        // Data retrieval
        List<WatchlistItem> watchlistItems = watchlistItemRepository.findByUser_UserId(userId);

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