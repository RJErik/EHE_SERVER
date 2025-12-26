package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AlertRemovalService implements AlertRemovalServiceInterface {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertRemovalService(AlertRepository alertRepository,
                               UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.remove",
            params = {"#alertId"}
    )
    @Override
    public void removeAlert(Integer userId, Integer alertId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (alertId == null) {
            throw new MissingAlertIdException();
        }

        // User validation
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Alert existence check
        Optional<Alert> alertOptional = alertRepository.findById(alertId);
        if (alertOptional.isEmpty()) {
            throw new AlertNotFoundException(alertId);
        }

        Alert alert = alertOptional.get();

        // Ownership verification
        if (!alert.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAlertAccessException(userId, alertId);
        }

        // Execute removal
        alertRepository.delete(alert);
    }
}