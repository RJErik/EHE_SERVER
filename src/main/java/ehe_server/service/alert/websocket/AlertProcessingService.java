package ehe_server.service.alert.websocket;

import ehe_server.entity.Alert;
import ehe_server.entity.MarketCandle;
import ehe_server.entity.MarketCandle.Timeframe;
import ehe_server.entity.PlatformStock;
import ehe_server.repository.AlertRepository;
import ehe_server.repository.MarketCandleRepository;
import ehe_server.service.intf.alert.websocket.AlertProcessingServiceInterface;
import ehe_server.service.intf.alert.websocket.TimeframeNavigatorInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AlertProcessingService implements AlertProcessingServiceInterface {

    private final AlertRepository alertRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final TimeframeNavigatorInterface timeframeNavigator;
    private final LoggingServiceInterface loggingService;

    public AlertProcessingService(
            AlertRepository alertRepository,
            MarketCandleRepository marketCandleRepository,
            TimeframeNavigatorInterface timeframeNavigator,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.timeframeNavigator = timeframeNavigator;
        this.loggingService = loggingService;
    }

    @Override
    public Optional<MarketCandle> checkAlertAgainstCandle(Alert alert, MarketCandle candle) {
        if (isConditionMet(alert, candle)) {
            loggingService.logAction("Alert #" + alert.getAlertId() + " condition met at " + candle.getTimestamp());
            return Optional.of(candle);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MarketCandle> checkAlertAgainstTimeframe(
            Alert alert,
            Timeframe timeframe,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        PlatformStock platformStock = alert.getPlatformStock();

        loggingService.logAction(String.format("Checking %s candles for alert #%d from %s to %s",
                timeframe, alert.getAlertId(), startTime, endTime));

        List<MarketCandle> candles = marketCandleRepository
                .findByPlatformStockAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                        platformStock, timeframe, startTime, endTime);

        if (candles.isEmpty()) {
            loggingService.logAction("No " + timeframe + " candles found for the specified period");
            return Optional.empty();
        }

        loggingService.logAction("Found " + candles.size() + " " + timeframe + " candles to check");

        // Check each candle for alert condition
        for (MarketCandle candle : candles) {
            Optional<MarketCandle> result = checkAlertAgainstCandle(alert, candle);
            if (result.isPresent()) {
                return result;
            }
        }

        // Try escalating to higher timeframe if at boundary
        return tryEscalateTimeframe(alert, candles, timeframe, endTime);
    }

    @Override
    public Optional<MarketCandle> getLatestMinuteCandle(Alert alert) {
        return Optional.ofNullable(
                marketCandleRepository.findTopByPlatformStockAndTimeframeOrderByTimestampDesc(
                        alert.getPlatformStock(), Timeframe.M1));
    }

    @Override
    public void deleteAlert(Alert alert) {
        alertRepository.delete(alert);
        loggingService.logAction("Alert #" + alert.getAlertId() + " deleted after triggering");
    }

    private boolean isConditionMet(Alert alert, MarketCandle candle) {
        BigDecimal priceToCheck = getRelevantPrice(alert, candle);
        BigDecimal threshold = alert.getThresholdValue();

        return switch (alert.getConditionType()) {
            case PRICE_ABOVE -> priceToCheck.compareTo(threshold) > 0;
            case PRICE_BELOW -> priceToCheck.compareTo(threshold) < 0;
        };
    }

    private BigDecimal getRelevantPrice(Alert alert, MarketCandle candle) {
        return switch (alert.getConditionType()) {
            case PRICE_ABOVE -> candle.getHighPrice();
            case PRICE_BELOW -> candle.getLowPrice();
        };
    }

    private Optional<MarketCandle> tryEscalateTimeframe(
            Alert alert,
            List<MarketCandle> candles,
            Timeframe currentTimeframe,
            LocalDateTime endTime) {

        if (candles.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime lastCandleTime = candles.getLast().getTimestamp();

        if (!timeframeNavigator.isAtBoundary(lastCandleTime, currentTimeframe)) {
            return Optional.empty();
        }

        Optional<Timeframe> nextTimeframe = timeframeNavigator.getNextHigherTimeframe(currentTimeframe);

        if (nextTimeframe.isEmpty()) {
            return Optional.empty();
        }

        loggingService.logAction("Escalating to " + nextTimeframe.get() + " from " + lastCandleTime);

        return checkAlertAgainstTimeframe(alert, nextTimeframe.get(), lastCandleTime, endTime);
    }
}