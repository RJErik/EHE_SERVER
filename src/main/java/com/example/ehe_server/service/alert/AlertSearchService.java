package com.example.ehe_server.service.alert;

import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public AlertSearchService(
            AlertRepository alertRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.alertRepository = alertRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> searchAlerts(String platform, String symbol, String conditionTypeStr) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Parse condition type if provided
            Alert.ConditionType conditionType = null;
            if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
                try {
                    conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
                } catch (IllegalArgumentException e) {
                    result.put("success", false);
                    result.put("message", "Invalid condition type");
                    loggingService.logAction("Alert search failed: Invalid condition type: " + conditionTypeStr);
                    return result;
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


            // Transform to response format
            List<Map<String, Object>> alertsList = alerts.stream()
                    .map(alert -> {
                        Map<String, Object> alertMap = new HashMap<>();
                        alertMap.put("id", alert.getAlertId());
                        alertMap.put("platform", alert.getPlatformStock().getPlatformName());
                        alertMap.put("symbol", alert.getPlatformStock().getStockSymbol());
                        alertMap.put("conditionType", alert.getConditionType().toString());
                        alertMap.put("thresholdValue", alert.getThresholdValue());
                        alertMap.put("dateCreated", alert.getDateCreated().format(DATE_FORMATTER));
                        alertMap.put("isActive", alert.isActive());
                        return alertMap;
                    })
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("alerts", alertsList);

            // Log success
            loggingService.logAction("Alert search successful, found " + alertsList.size() + " alerts");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error searching alerts: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching alerts");
        }

        return result;
    }
}
