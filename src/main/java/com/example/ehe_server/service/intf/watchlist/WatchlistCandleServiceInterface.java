package com.example.ehe_server.service.intf.watchlist;

import com.example.ehe_server.dto.WatchlistCandleResponse;

import java.util.List;
import java.util.Map;

public interface WatchlistCandleServiceInterface {
    List<WatchlistCandleResponse> getLatestCandles(Integer userId);
}
