package ehe_server.service.intf.alert.websocket;

import ehe_server.entity.Alert;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.MarketCandle.Timeframe;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AlertProcessingServiceInterface {

    /**
     * Check if alert condition is met against a candle.
     * Returns the triggering candle if condition is met, empty otherwise.
     * Does NOT send notifications or delete alerts.
     */
    Optional<MarketCandle> checkAlertAgainstCandle(Alert alert, MarketCandle candle);

    /**
     * Check alert against a timeframe range.
     * Returns the first triggering candle if condition is met, empty otherwise.
     * Does NOT send notifications or delete alerts.
     */
    Optional<MarketCandle> checkAlertAgainstTimeframe(
            Alert alert,
            Timeframe timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * Get the latest minute candle for an alert's platform stock.
     */
    Optional<MarketCandle> getLatestMinuteCandle(Alert alert);

    /**
     * Delete an alert after it has been triggered.
     */
    void deleteAlert(Alert alert);
}