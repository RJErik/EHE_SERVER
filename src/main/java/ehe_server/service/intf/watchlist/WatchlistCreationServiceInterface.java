package ehe_server.service.intf.watchlist;

import ehe_server.dto.WatchlistResponse;

public interface WatchlistCreationServiceInterface {
    /**
     * Adds a new item to the current user's watchlistitem
     * @param userId The user that has initiated the service
     * @param platform The trading platform name
     * @param symbol The stock symbol
     * @return Map containing success status and added item details
     */
    WatchlistResponse createWatchlistItem(Integer userId, String platform, String symbol);
}
