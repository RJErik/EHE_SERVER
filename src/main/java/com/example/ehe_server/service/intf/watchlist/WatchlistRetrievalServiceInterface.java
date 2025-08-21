package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistRetrievalResponse;

import java.util.List;
import java.util.Map;

/**
 * Interface for watchlist retrieval operations
 */
public interface WatchlistRetrievalServiceInterface {
    /**
     * Retrieves all watchlist items for the current user
     * @param userId The user that has initiated the service
     * @return Map containing success status and list of watchlist items
     */
    List<WatchlistRetrievalResponse> getWatchlistItems(Integer userId);
}