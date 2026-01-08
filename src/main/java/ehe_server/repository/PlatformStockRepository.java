package ehe_server.repository;

import ehe_server.entity.PlatformStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlatformStockRepository extends JpaRepository<PlatformStock, Integer> {

    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.stock " +
            "JOIN FETCH ps.platform p " +
            "WHERE p.platformName = :platformName")
    List<PlatformStock> findByPlatformPlatformName(String platformName);

    List<PlatformStock> findByPlatformPlatformNameOrderByStockStockSymbolAsc(String platformName);

    List<PlatformStock> findByPlatformPlatformNameAndStockStockSymbol(String platformName, String stockName);

    boolean existsByPlatformPlatformName(String platformName);

    @Query("SELECT DISTINCT p.platformName FROM Platform p " +
            "JOIN PlatformStock ps ON ps.platform = p " +
            "ORDER BY p.platformName ASC")
    List<String> findDistinctPlatformNames();

    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.platform p " +
            "JOIN FETCH ps.stock s " +
            "WHERE p.platformName = :platformName " +
            "AND s.stockSymbol IN :stockNames")
    List<PlatformStock> findByPlatformNameAndStockNameIn(
            @Param("platformName") String platformName,
            @Param("stockNames") Set<String> stockNames);

    @Query("SELECT ps FROM PlatformStock ps " +
            "JOIN FETCH ps.stock s " +
            "JOIN FETCH ps.platform p " +
            "WHERE s.stockSymbol = :stockName AND p.platformName = :platformName")
    Optional<PlatformStock> findByStockNameAndPlatformName(
            @Param("stockName") String stockName,
            @Param("platformName") String platformName);
}