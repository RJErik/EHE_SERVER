package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistResponse;

import java.util.List;
import java.util.Map;

/**
 * Interface for watchlistitem retrieval operations
 */
public interface WatchlistRetrievalServiceInterface {
    /**
     * Retrieves all watchlistitem items for the current user
     * @param userId The user that has initiated the service
     * @return Map containing success status and list of watchlistitem items
     */
    List<WatchlistResponse> getWatchlistItems(Integer userId);
}