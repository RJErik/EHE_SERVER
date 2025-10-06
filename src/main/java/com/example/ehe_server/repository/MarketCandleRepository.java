package com.example.ehe_server.repository;

import com.example.ehe_server.entity.MarketCandle;
import com.example.ehe_server.entity.MarketCandle.Timeframe;
import com.example.ehe_server.entity.PlatformStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Integer> {

    List<MarketCandle> findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
            PlatformStock platformStock,
            Timeframe timeframe,
            LocalDateTime startDate,
            LocalDateTime endDate);

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
}
