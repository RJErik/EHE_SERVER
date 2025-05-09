package com.example.ehe_server.service.intf.watchlist;

import java.util.Map;

public interface WatchlistSearchServiceInterface {
    Map<String, Object> searchWatchlistItems(String platform, String symbol);
}
