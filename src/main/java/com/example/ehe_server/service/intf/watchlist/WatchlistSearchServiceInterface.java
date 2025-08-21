package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistSearchResponse;

import java.util.List;
import java.util.Map;

public interface WatchlistSearchServiceInterface {
    List<WatchlistSearchResponse> searchWatchlistItems(Integer userId, String platform, String symbol);
}
