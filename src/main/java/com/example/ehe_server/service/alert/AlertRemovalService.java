package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.AlertNotFoundException;
import com.example.ehe_server.exception.custom.UnauthorizedAlertAccessException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class AlertRemovalService implements AlertRemovalServiceInterface {

    private final AlertRepository alertRepository;

    public AlertRemovalService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.remove",
            params = {"#alertId"}
    )
    @Override
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

        // Remove the alert
        alertRepository.delete(alert);
    }
}