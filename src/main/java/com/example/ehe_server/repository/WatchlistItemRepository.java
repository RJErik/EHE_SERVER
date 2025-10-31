package com.example.ehe_server.repository;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Integer> {
    List<WatchlistItem> findByWatchlist(Watchlist watchlist);
    List<WatchlistItem> findByWatchlistAndPlatformStock_PlatformName(Watchlist watchlist, String platform);
    @Query("SELECT wi FROM WatchlistItem wi WHERE " +
            "wi.watchlist = :watchlist AND " +
            "(:platform IS NULL OR wi.platformStock.platformName = :platform) AND " +
            "(:symbol IS NULL OR wi.platformStock.stockSymbol = :symbol)")
    List<WatchlistItem> searchWatchlistItems(
            @Param("watchlist") Watchlist watchlist,
            @Param("platform") String platform,
            @Param("symbol") String symbol
    );
    Optional<WatchlistItem> findByWatchlistAndPlatformStock(Watchlist watchlist, PlatformStock platformStock);
}
