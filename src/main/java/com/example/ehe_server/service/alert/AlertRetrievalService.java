package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AlertRetrievalService implements AlertRetrievalServiceInterface {

    private final AlertRepository alertRepository;

    public AlertRetrievalService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AlertResponse> getAlerts(Integer userId) {

        // Data retrieval and response mapping
        List<Alert> alerts = alertRepository.findByUser_UserIdOrderByDateCreatedDesc(userId);

        return alerts.stream()
                .map(alert -> new AlertResponse(
                        alert.getAlertId(),
                        alert.getPlatformStock().getPlatform().getPlatformName(),
                        alert.getPlatformStock().getStock().getStockSymbol(),
                        alert.getConditionType(),
                        alert.getThresholdValue(),
                        alert.getDateCreated()
                ))
                .collect(Collectors.toList());
    }
}