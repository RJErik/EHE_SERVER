package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertRetrievalResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertRetrievalService implements AlertRetrievalServiceInterface {

    private final AlertRepository alertRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertRetrievalService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AlertRetrievalResponse> getAlerts(Integer userId) {
        // Get all active alerts for the user
        List<Alert> alerts = alertRepository.findByUser_UserIdAndActiveTrue(userId);

        return alerts.stream()
                .map(alert -> new AlertRetrievalResponse(
                        alert.getAlertId(),
                        alert.getPlatformStock().getPlatformName(),
                        alert.getPlatformStock().getStockSymbol(),
                        alert.getConditionType().toString(),
                        alert.getThresholdValue(),
                        alert.getDateCreated().format(DATE_FORMATTER),
                        alert.isActive()
                ))
                .collect(Collectors.toList());
    }
}