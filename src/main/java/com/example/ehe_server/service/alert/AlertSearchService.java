package com.example.ehe_server.service.alert;

import com.example.ehe_server.dto.AlertSearchResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;
    private final LoggingServiceInterface loggingService;

    public AlertSearchService(
            AlertRepository alertRepository,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.loggingService = loggingService;
    }

    @Override
    public List<AlertSearchResponse> searchAlerts(Integer userId, String platform, String symbol, String conditionTypeStr) {
        // Parse condition type if provided
        Alert.ConditionType conditionType = null;
        if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
            try {
                conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidConditionTypeException(conditionTypeStr);
            }
        }

        // Apply search filters
        List<Alert> alerts;
        if (platform != null && !platform.trim().isEmpty() &&
                symbol != null && !symbol.trim().isEmpty() &&
                conditionType != null) {
            // Search by platform, symbol and condition type
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndPlatformStock_StockSymbolAndConditionTypeAndActiveTrue(
                    userId, platform, symbol, conditionType);
            loggingService.logAction("Searching alerts with platform=" + platform + ", symbol=" + symbol + ", conditionType=" + conditionType);
        } else if (platform != null && !platform.trim().isEmpty() &&
                symbol != null && !symbol.trim().isEmpty()) {
            // Search by platform and symbol
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndPlatformStock_StockSymbolAndActiveTrue(
                    userId, platform, symbol);
            loggingService.logAction("Searching alerts with platform=" + platform + " and symbol=" + symbol);
        } else if (platform != null && !platform.trim().isEmpty() &&
                conditionType != null) {
            // Search by platform and condition type
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndConditionTypeAndActiveTrue(
                    userId, platform, conditionType);
            loggingService.logAction("Searching alerts with platform=" + platform + " and conditionType=" + conditionType);
        } else if (symbol != null && !symbol.trim().isEmpty() &&
                conditionType != null) {
            // Search by symbol and condition type
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_StockSymbolAndConditionTypeAndActiveTrue(
                    userId, symbol, conditionType);
            loggingService.logAction("Searching alerts with symbol=" + symbol + " and conditionType=" + conditionType);
        } else if (platform != null && !platform.trim().isEmpty()) {
            // Search by platform only
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndActiveTrue(userId, platform);
            loggingService.logAction("Searching alerts with platform=" + platform);
        } else if (symbol != null && !symbol.trim().isEmpty()) {
            // Search by symbol only
            alerts = alertRepository.findByUser_UserIdAndPlatformStock_StockSymbolAndActiveTrue(userId, symbol);
            loggingService.logAction("Searching alerts with symbol=" + symbol);
        } else if (conditionType != null) {
            // Search by condition type only
            alerts = alertRepository.findByUser_UserIdAndConditionTypeAndActiveTrue(userId, conditionType);
            loggingService.logAction("Searching alerts with conditionType=" + conditionType);
        } else {
            // No filters, get all active alerts for the user
            alerts = alertRepository.findByUser_UserIdAndActiveTrue(userId);
            loggingService.logAction("Searching alerts with no filters");
        }

        // Transform to AlertDto using the AlertMapper
        // Transform to AlertSearchResponse using an inline lambda
        List<AlertSearchResponse> alertSearchResponses = alerts.stream()
                .map(alert -> {
                    AlertSearchResponse alertSearchResponse = new AlertSearchResponse();
                    alertSearchResponse.setId(alert.getAlertId());
                    alertSearchResponse.setPlatform(alert.getPlatformStock().getPlatformName());
                    alertSearchResponse.setSymbol(alert.getPlatformStock().getStockSymbol());
                    alertSearchResponse.setConditionType(alert.getConditionType().name());
                    alertSearchResponse.setThresholdValue(alert.getThresholdValue());
                    alertSearchResponse.setDateCreated(alert.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    alertSearchResponse.setActive(alert.isActive());
                    return alertSearchResponse;
                })
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Alert search successful, found " + alertSearchResponses.size() + " alerts");

        // Return the list of DTOs directly
        return alertSearchResponses;
    }
}