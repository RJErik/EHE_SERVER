package com.example.ehe_server.service.intf.alert.websocket;

import com.example.ehe_server.entity.MarketCandle.Timeframe;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TimeframeNavigatorInterface {

    /**
     * Check if the given timestamp falls on a boundary for transitioning to the next higher timeframe.
     *
     * @param time      the timestamp to check
     * @param timeframe the current timeframe
     * @return true if at a boundary, false otherwise
     */
    boolean isAtBoundary(LocalDateTime time, Timeframe timeframe);

    /**
     * Get the next higher timeframe from the current one.
     *
     * @param current the current timeframe
     * @return an Optional containing the next higher timeframe, or empty if already at the highest
     */
    Optional<Timeframe> getNextHigherTimeframe(Timeframe current);
}