package ehe_server.service.alert.websocket;

import ehe_server.entity.Alert;
import ehe_server.entity.MarketCandle;
import ehe_server.repository.AlertRepository;
import ehe_server.service.intf.alert.websocket.AlertInitialCheckServiceInterface;
import ehe_server.service.intf.alert.websocket.AlertNotificationServiceInterface;
import ehe_server.service.intf.alert.websocket.AlertProcessingServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AlertInitialCheckService implements AlertInitialCheckServiceInterface {

    private static final int UTC_OFFSET_HOURS = 1;

    private final AlertRepository alertRepository;
    private final AlertProcessingServiceInterface processingService;
    private final AlertNotificationServiceInterface notificationService;
    private final LoggingServiceInterface loggingService;

    public AlertInitialCheckService(
            AlertRepository alertRepository,
            AlertProcessingServiceInterface processingService,
            AlertNotificationServiceInterface notificationService,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.processingService = processingService;
        this.notificationService = notificationService;
        this.loggingService = loggingService;
    }

    @Override
    @Async
    @Transactional
    public void performInitialAlertCheckAsync(AlertSubscription subscription) {
        try {
            performInitialAlertCheck(subscription);
        } catch (Exception e) {
            loggingService.logAction("Error during initial alert check for subscription " +
                    subscription.getId() + ": " + e.getMessage());
        }
    }

    private void performInitialAlertCheck(AlertSubscription subscription) {
        List<Alert> userAlerts = alertRepository.findByUser_UserId(subscription.getUserId());

        if (userAlerts.isEmpty()) {
            loggingService.logAction("No active alerts found for initial check");
            subscription.markInitialCheckCompleted();
            return;
        }

        loggingService.logAction("Starting initial check for " + userAlerts.size() + " alerts");

        for (Alert alert : userAlerts) {
            processInitialAlertCheck(alert, subscription);
        }

        subscription.markInitialCheckCompleted();
        subscription.updateLastCheckedMinuteCandle(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        loggingService.logAction("Completed initial alert check for subscription: " + subscription.getId());
    }

    private void processInitialAlertCheck(Alert alert, AlertSubscription subscription) {
        LocalDateTime checkStartTime = alert.getDateCreated()
                .truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(1)
                .minusHours(UTC_OFFSET_HOURS);

        loggingService.logAction(String.format("Checking alert #%d for %s:%s starting from %s",
                alert.getAlertId(),
                alert.getPlatformStock().getPlatform().getPlatformName(),
                alert.getPlatformStock().getStock().getStockSymbol(),
                checkStartTime));

        Optional<MarketCandle> triggeringCandle = processingService.checkAlertAgainstTimeframe(
                alert,
                MarketCandle.Timeframe.M1,
                checkStartTime,
                LocalDateTime.now());

        triggeringCandle.ifPresent(candle -> handleTriggeredAlert(alert, candle, subscription));
    }

    private void handleTriggeredAlert(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription) {
        notificationService.sendTriggeredNotification(alert, triggeringCandle, subscription);
        processingService.deleteAlert(alert);
        loggingService.logAction("Alert #" + alert.getAlertId() + " triggered and deactivated");
    }
}