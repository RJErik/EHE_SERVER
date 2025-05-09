package com.example.ehe_server.repository;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Integer> {
    List<WatchlistItem> findByWatchlist(Watchlist watchlist);
    List<WatchlistItem> findByWatchlistAndPlatformStock_PlatformName(Watchlist watchlist, String platform);
    List<WatchlistItem> findByWatchlistAndPlatformStock_StockSymbol(Watchlist watchlist, String symbol);
    List<WatchlistItem> findByWatchlistAndPlatformStock_PlatformNameAndPlatformStock_StockSymbol(
            Watchlist watchlist, String platform, String symbol);
    Optional<WatchlistItem> findByWatchlistAndPlatformStock(Watchlist watchlist, PlatformStock platformStock);
}
