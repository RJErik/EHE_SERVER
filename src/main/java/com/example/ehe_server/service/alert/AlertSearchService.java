package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;

    public AlertSearchService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.search",
            params = {
                    "#platform",
                    "#symbol",
                    "#conditionType",
                    "#result.size()"}
    )
    @Override
    public List<AlertResponse> searchAlerts(Integer userId, String platform, String symbol, Alert.ConditionType conditionType) {

        // Data retrieval
        List<Alert> alerts = alertRepository.searchAlerts(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null,
                conditionType
        );

        // Response mapping
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