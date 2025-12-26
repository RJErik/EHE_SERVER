package com.example.ehe_server.repository;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.MarketCandle.Timeframe;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.service.stock.ICandleWithSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Integer> {

    @Query("SELECT m FROM MarketCandle m " +
            "WHERE m.platformStock IN :stocks " +
            "AND m.timeframe = :timeframe " +
            "AND m.timestamp = (" +
            "    SELECT MAX(m2.timestamp) " +
            "    FROM MarketCandle m2 " +
            "    WHERE m2.platformStock = m.platformStock " +
            "    AND m2.timeframe = :timeframe" +
            ")")
    List<MarketCandle> findLatestCandlesForStocks(
            @Param("stocks") List<PlatformStock> stocks,
            @Param("timeframe") MarketCandle.Timeframe timeframe
    );

    // Replaced the simple JPA findBy... with a native query to calculate sequence
    @Query(value = """
        SELECT * FROM (
            SELECT mc.market_candle_id as marketCandleId, 
                   mc.timestamp, 
                   mc.open_price as openPrice, 
                   mc.close_price as closePrice, 
                   mc.high_price as highPrice, 
                   mc.low_price as lowPrice, 
                   mc.volume,
                   ROW_NUMBER() OVER (
                       ORDER BY mc.timestamp ASC
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.timestamp BETWEEN :startDate AND :endDate
        ORDER BY sequenced_data.timestamp ASC
        """, nativeQuery = true)
    List<ICandleWithSequence> findCandlesByDateRangeWithSequence(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find the latest candle for a specific stock and timeframe
    MarketCandle findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
            PlatformStock platformStock,
            Timeframe timeframe);

    Optional<MarketCandle> findByPlatformStockAndTimeframeAndTimestampEquals(
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            LocalDateTime timestamp);

    List<MarketCandle> findAllByPlatformStockAndTimeframeAndTimestamp(
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            LocalDateTime timestamp);

    MarketCandle findByPlatformStockAndTimeframeAndTimestamp(
            PlatformStock platformStock,
            Timeframe timeframe,
            LocalDateTime timestamp);

    /**
     * Finds the single latest candle with its calculated sequence number.
     * Uses LIMIT 1 on the sequenced data.
     */
    @Query(value = """
        SELECT * FROM (
            SELECT mc.market_candle_id as marketCandleId, 
                   mc.timestamp, 
                   mc.open_price as openPrice, 
                   mc.close_price as closePrice, 
                   mc.high_price as highPrice, 
                   mc.low_price as lowPrice, 
                   mc.volume,
                   ROW_NUMBER() OVER (
                       ORDER BY mc.timestamp ASC
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        ORDER BY sequenced_data.timestamp DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ICandleWithSequence> findLatestCandleWithSequence(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe);

    /**
     * Finds a specific candle by timestamp with its calculated sequence number.
     */
    @Query(value = """
        SELECT * FROM (
            SELECT mc.market_candle_id as marketCandleId, 
                   mc.timestamp, 
                   mc.open_price as openPrice, 
                   mc.close_price as closePrice, 
                   mc.high_price as highPrice, 
                   mc.low_price as lowPrice, 
                   mc.volume,
                   ROW_NUMBER() OVER (
                       ORDER BY mc.timestamp ASC
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.timestamp = :timestamp
        """, nativeQuery = true)
    Optional<ICandleWithSequence> findCandleWithSequenceByTimestamp(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("timestamp") LocalDateTime timestamp);

    @Query(value = "SELECT * FROM market_candle mc " +
            "WHERE mc.timeframe = '1d' " +
            "AND CAST(mc.timestamp AS date) = CURRENT_DATE " +
            "AND mc.open_price > 0 " +
            "ORDER BY ABS((mc.close_price - mc.open_price) / mc.open_price) DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<MarketCandle> findTopTenDailyCandlesByPercentageChange();

    @Query(value = "SELECT * FROM market_candle mc " +
            "WHERE mc.timeframe = '1d' " +
            "AND CAST(mc.timestamp AS date) = CURRENT_DATE " +
            "AND mc.open_price > 0 " +
            "ORDER BY (mc.close_price - mc.open_price) / mc.open_price ASC " +
            "LIMIT 10",
            nativeQuery = true)
    List<MarketCandle> findBottomTenDailyCandlesByPercentageChange();

    @Query(value = """
        SELECT * FROM (
            SELECT mc.market_candle_id as marketCandleId, 
                   mc.timestamp, 
                   mc.open_price as openPrice, 
                   mc.close_price as closePrice, 
                   mc.high_price as highPrice, 
                   mc.low_price as lowPrice, 
                   mc.volume,
                   ROW_NUMBER() OVER (
                       ORDER BY mc.timestamp ASC
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.sequence BETWEEN :fromSequence AND :toSequence
        ORDER BY sequenced_data.timestamp ASC
        """, nativeQuery = true)
    List<ICandleWithSequence> findByStockAndTimeframeAndSequenceRange(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("fromSequence") long fromSequence,
            @Param("toSequence") long toSequence
    );

    List<MarketCandle> findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(PlatformStock platformStock, Timeframe timeframe, LocalDateTime startTime, LocalDateTime endTime);
}