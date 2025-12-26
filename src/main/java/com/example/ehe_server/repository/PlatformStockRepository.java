package com.example.ehe_server.repository;

import com.example.ehe_server.entity.Platform;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlatformStockRepository extends JpaRepository<PlatformStock, Integer> {

    // Find by platform
    List<PlatformStock> findByPlatform(Platform platform);

    List<PlatformStock> findByPlatformPlatformId(Integer platformId);

    List<PlatformStock> findByPlatformPlatformName(String platformName);

    List<PlatformStock> findByPlatformPlatformNameOrderByStockStockNameAsc(String platformName);

    // Find by stock
    List<PlatformStock> findByStock(Stock stock);

    List<PlatformStock> findByStockStockId(Integer stockId);

    List<PlatformStock> findByStockStockName(String stockName);

    // Find by platform and stock combination
    Optional<PlatformStock> findByPlatformAndStock(Platform platform, Stock stock);

    Optional<PlatformStock> findByPlatformPlatformIdAndStockStockId(Integer platformId, Integer stockId);

    // Returns List for multiple potential matches
    List<PlatformStock> findByPlatformPlatformNameAndStockStockName(String platformName, String stockName);

    // Existence checks
    boolean existsByPlatformPlatformName(String platformName);

    boolean existsByStockStockName(String stockName);

    boolean existsByPlatformPlatformIdAndStockStockId(Integer platformId, Integer stockId);

    boolean existsByPlatformPlatformNameAndStockStockName(String platformName, String stockName);

    // Get distinct platform names (now fetched through join)
    @Query("SELECT DISTINCT p.platformName FROM Platform p " +
            "JOIN PlatformStock ps ON ps.platform = p " +
            "ORDER BY p.platformName ASC")
    List<String> findDistinctPlatformNames();

    // Get distinct stock Names (now fetched through join)
    @Query("SELECT DISTINCT s.stockName FROM Stock s " +
            "JOIN PlatformStock ps ON ps.stock = s " +
            "ORDER BY s.stockName ASC")
    List<String> findDistinctStockNames();

    // Find by platform name and multiple stock Names
    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.platform p " +
            "JOIN FETCH ps.stock s " +
            "WHERE p.platformName = :platformName " +
            "AND s.stockName IN :stockNames")
    List<PlatformStock> findByPlatformNameAndStockNameIn(
            @Param("platformName") String platformName,
            @Param("stockNames") Set<String> stockNames);

    // Find all with eager fetch (useful to avoid N+1 queries)
    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.platform " +
            "JOIN FETCH ps.stock")
    List<PlatformStock> findAllWithPlatformAndStock();

    // Find by platform with eager fetch
    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.platform p " +
            "JOIN FETCH ps.stock s " +
            "WHERE p.platformName = :platformName " +
            "ORDER BY s.stockName ASC")
    List<PlatformStock> findByPlatformNameWithStock(@Param("platformName") String platformName);

    // Find single result by stock name and platform name (with eager fetch)
    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.stock s " +
            "JOIN FETCH ps.platform p " +
            "WHERE s.stockName = :stockName AND p.platformName = :platformName")
    Optional<PlatformStock> findByStockNameAndPlatformName(
            @Param("stockName") String stockName,
            @Param("platformName") String platformName);
}