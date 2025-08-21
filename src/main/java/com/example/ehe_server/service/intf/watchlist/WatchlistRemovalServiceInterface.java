package com.example.ehe_server.service.intf.watchlist;

public interface WatchlistRemovalServiceInterface {
    /**
     * Removes an item from the current user's watchlist
     * @param userId The user that has initiated the service
     * @param watchlistItemId The ID of the watchlist item to remove
     * @return Map containing success status and removal confirmation
     */
    void removeWatchlistItem(Integer userId, Integer watchlistItemId);
}
