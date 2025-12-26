package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AlertRetrievalService implements AlertRetrievalServiceInterface {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertRetrievalService(AlertRepository alertRepository,
                                 UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AlertResponse> getAlerts(Integer userId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval and response mapping
        List<Alert> alerts = alertRepository.findByUser_UserIdOrderByDateCreatedDesc(userId);

        return alerts.stream()
                .map(alert -> new AlertResponse(
                        alert.getAlertId(),
                        alert.getPlatformStock().getPlatform().getPlatformName(),
                        alert.getPlatformStock().getStock().getStockName(),
                        alert.getConditionType().toString(),
                        alert.getThresholdValue(),
                        alert.getDateCreated().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}