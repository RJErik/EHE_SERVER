package ehe_server.service.intf.watchlist;

import ehe_server.dto.WatchlistResponse;

import java.util.List;

public interface WatchlistSearchServiceInterface {
    List<WatchlistResponse> searchWatchlistItems(Integer userId, String platform, String symbol);
}
