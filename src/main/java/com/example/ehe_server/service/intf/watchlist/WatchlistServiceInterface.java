package com.example.ehe_server.service.intf.watchlist;

import java.util.Map;

public interface WatchlistServiceInterface {
    Map<String, Object> getWatchlistItems();
    Map<String, Object> addWatchlistItem(String platform, String symbol);
    Map<String, Object> removeWatchlistItem(Integer watchlistItemId);
}
