package ehe_server.repository;

import ehe_server.entity.PlatformStock;
import ehe_server.entity.WatchlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Integer> {
    @EntityGraph(attributePaths = {"platformStock", "platformStock.platform", "platformStock.stock"})
    List<WatchlistItem> findByUser_UserId(Integer userId);

    @EntityGraph(attributePaths = {"platformStock", "platformStock.platform", "platformStock.stock"})
    @Query("SELECT wi FROM WatchlistItem wi WHERE " +
            "wi.user.userId = :userId AND " +
            "(:platform IS NULL OR wi.platformStock.platform.platformName = :platform) AND " +
            "(:symbol IS NULL OR wi.platformStock.stock.stockSymbol = :symbol)")
    List<WatchlistItem> searchWatchlistItems(
            @Param("userId") Integer userId,
            @Param("platform") String platform,
            @Param("symbol") String symbol
    );

    Optional<WatchlistItem> findByUser_UserIdAndPlatformStock(Integer userId, PlatformStock platformStock);
}