package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Alert existence check
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        // Ownership verification
        if (!alert.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAlertAccessException(userId, alertId);
        }

        // Execute removal
        alertRepository.delete(alert);
    }
}