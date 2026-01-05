package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistResponse;

import java.util.List;

public interface WatchlistRetrievalServiceInterface {
    List<WatchlistResponse> getWatchlistItems(Integer userId);
}