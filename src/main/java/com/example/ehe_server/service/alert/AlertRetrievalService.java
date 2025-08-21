package com.example.ehe_server.service.alert;

import com.example.ehe_server.dto.AlertRetrievalResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertRetrievalService implements AlertRetrievalServiceInterface {

    private final AlertRepository alertRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertRetrievalService(
            AlertRepository alertRepository,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.loggingService = loggingService;
    }

    public List<AlertRetrievalResponse> getAlerts(Integer userId) {
        // Get all active alerts for the user
        List<Alert> alerts = alertRepository.findByUser_UserIdAndActiveTrue(userId);

        // Log success
        loggingService.logAction("Alerts retrieved successfully");

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