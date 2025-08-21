package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistCreationResponse;

public interface WatchlistCreationServiceInterface {
    /**
     * Adds a new item to the current user's watchlist
     * @param userId The user that has initiated the service
     * @param platform The trading platform name
     * @param symbol The stock symbol
     * @return Map containing success status and added item details
     */
    WatchlistCreationResponse createWatchlistItem(Integer userId, String platform, String symbol);
}
