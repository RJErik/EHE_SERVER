package com.example.ehe_server.service.alert;

import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.AlertNotFoundException;
import com.example.ehe_server.exception.custom.UnauthorizedAlertAccessException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class AlertRemovalService implements AlertRemovalServiceInterface {

    private final AlertRepository alertRepository;
    private final LoggingServiceInterface loggingService;

    public AlertRemovalService(
            AlertRepository alertRepository,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.loggingService = loggingService;
    }

    public void removeAlert(Integer userId, Integer alertId) {
        // Check if the alert exists
        Optional<Alert> alertOptional = alertRepository.findById(alertId);
        if (alertOptional.isEmpty()) {
            throw new AlertNotFoundException(alertId);
        }

        Alert alert = alertOptional.get();

        // Verify the alert belongs to the user
        if (!alert.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAlertAccessException(userId, alertId);
        }

        // Get alert details for logging
        String platform = alert.getPlatformStock().getPlatformName();
        String symbol = alert.getPlatformStock().getStockSymbol();
        Alert.ConditionType conditionType = alert.getConditionType();
        BigDecimal thresholdValue = alert.getThresholdValue();

        // Remove the alert
        alertRepository.delete(alert);

        // Log success
        loggingService.logAction("Removed alert: " + platform + "/" + symbol + " " + conditionType + " " + thresholdValue + " (ID: " + alertId + ")");
    }
}