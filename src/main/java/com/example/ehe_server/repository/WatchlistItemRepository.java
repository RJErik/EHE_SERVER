package com.example.ehe_server.repository;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Integer> {
    List<WatchlistItem> findByUser_UserId(Integer userId);

    List<WatchlistItem> findByUser_UserIdAndPlatformStock_PlatformName(Integer userId, String platform);

    @Query("SELECT wi FROM WatchlistItem wi WHERE " +
            "wi.user.userId = :userId AND " +
            "(:platform IS NULL OR wi.platformStock.platformName = :platform) AND " +
            "(:symbol IS NULL OR wi.platformStock.stockSymbol = :symbol)")
    List<WatchlistItem> searchWatchlistItems(
            @Param("userId") Integer userId,
            @Param("platform") String platform,
            @Param("symbol") String symbol
    );

    Optional<WatchlistItem> findByUser_UserIdAndPlatformStock(Integer userId, PlatformStock platformStock);
}