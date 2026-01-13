package ehe_server.repository;

import ehe_server.entity.MarketCandle;
import ehe_server.entity.MarketCandle.Timeframe;
import ehe_server.entity.PlatformStock;
import ehe_server.service.stock.CandleWithSequenceInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Integer> {

    List<MarketCandle> findByPlatformStockAndTimeframeAndTimestampBetween(
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * Fetches the single latest candle for each provided stock within the given timeframe.
     */
    @Query("SELECT mc FROM MarketCandle mc " +
            "WHERE mc.platformStock IN :stocks " +
            "AND mc.timeframe = :timeframe " +
            "AND mc.timestamp = (" +
            "    SELECT MAX(sub.timestamp) " +
            "    FROM MarketCandle sub " +
            "    WHERE sub.platformStock = mc.platformStock " +
            "    AND sub.timeframe = :timeframe" +
            ")")
    List<MarketCandle> findLatestCandlesForStocks(
            @Param("stocks") List<PlatformStock> stocks,
            @Param("timeframe") MarketCandle.Timeframe timeframe
    );

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
                       ORDER BY mc.timestamp
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.timestamp BETWEEN :startDate AND :endDate
        ORDER BY sequenced_data.timestamp
        """, nativeQuery = true)
    List<CandleWithSequenceInterface> findCandlesByDateRangeWithSequence(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    MarketCandle findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
            PlatformStock platformStock,
            Timeframe timeframe);

    Optional<MarketCandle> findByPlatformStockAndTimeframeAndTimestampEquals(
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            LocalDateTime timestamp);

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
                       ORDER BY mc.timestamp
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        ORDER BY sequenced_data.timestamp DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<CandleWithSequenceInterface> findLatestCandleWithSequence(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe);

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
                       ORDER BY mc.timestamp
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.timestamp = :timestamp
        """, nativeQuery = true)
    Optional<CandleWithSequenceInterface> findCandleWithSequenceByTimestamp(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("timestamp") LocalDateTime timestamp);

    @Query(value = """
    SELECT mc.* FROM market_candle mc
    INNER JOIN platform_stock ps ON mc.platform_stock_id = ps.platform_stock_id
    INNER JOIN platform p ON ps.platform_id = p.platform_id
    INNER JOIN stock s ON ps.stock_id = s.stock_id
    WHERE mc.timeframe = '1d' AND CAST(mc.timestamp AS date) = CURRENT_DATE
    AND mc.open_price > 0 
    ORDER BY (mc.close_price - mc.open_price) / mc.open_price DESC 
    LIMIT 10
    """, nativeQuery = true)
    List<MarketCandle> findTopTenDailyCandlesByPercentageChange();



    @Query(value = """
    SELECT mc.* FROM market_candle mc
    INNER JOIN platform_stock ps ON mc.platform_stock_id = ps.platform_stock_id
    INNER JOIN platform p ON ps.platform_id = p.platform_id
    INNER JOIN stock s ON ps.stock_id = s.stock_id
    WHERE mc.timeframe = '1d' AND CAST(mc.timestamp AS date) = CURRENT_DATE AND mc.open_price > 0
    ORDER BY (mc.close_price - mc.open_price) / mc.open_price
    LIMIT 10
    """, nativeQuery = true)
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
                       ORDER BY mc.timestamp
                   ) as sequence
            FROM market_candle mc
            WHERE mc.platform_stock_id = :stockId
            AND mc.timeframe = :#{#timeframe.value}
        ) as sequenced_data
        WHERE sequenced_data.sequence BETWEEN :fromSequence AND :toSequence
        ORDER BY sequenced_data.timestamp
        """, nativeQuery = true)
    List<CandleWithSequenceInterface> findByStockAndTimeframeAndSequenceRange(
            @Param("stockId") Integer stockId,
            @Param("timeframe") Timeframe timeframe,
            @Param("fromSequence") long fromSequence,
            @Param("toSequence") long toSequence
    );

    List<MarketCandle> findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(PlatformStock platformStock, Timeframe timeframe, LocalDateTime startTime, LocalDateTime endTime);

    List<MarketCandle> findByPlatformStockAndTimeframeAndTimestampIn(
            PlatformStock platformStock,
            MarketCandle.Timeframe timeframe,
            List<LocalDateTime> timestamps
    );

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO market_candle 
        (platform_stock_id, timeframe, timestamp, open_price, high_price, low_price, close_price, volume)
    VALUES 
        (:platformStockId, :timeframe, :timestamp, :openPrice, :highPrice, :lowPrice, :closePrice, :volume)
    ON CONFLICT (platform_stock_id, timeframe, timestamp)
    DO UPDATE SET 
        open_price = EXCLUDED.open_price,
        high_price = EXCLUDED.high_price,
        low_price = EXCLUDED.low_price,
        close_price = EXCLUDED.close_price,
        volume = EXCLUDED.volume
    """, nativeQuery = true)
    void upsertCandle(
            @Param("platformStockId") Integer platformStockId,
            @Param("timeframe") String timeframe,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("openPrice") BigDecimal openPrice,
            @Param("highPrice") BigDecimal highPrice,
            @Param("lowPrice") BigDecimal lowPrice,
            @Param("closePrice") BigDecimal closePrice,
            @Param("volume") BigDecimal volume);

}