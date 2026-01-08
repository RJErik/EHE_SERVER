package ehe_server.service.intf.watchlist;

import ehe_server.dto.WatchlistCandleResponse;

import java.util.List;

public interface WatchlistCandleServiceInterface {
    List<WatchlistCandleResponse> getLatestCandles(Integer userId);
}
