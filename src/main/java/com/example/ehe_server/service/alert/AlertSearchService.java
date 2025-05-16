package com.example.ehe_server.service.alert;

import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertSearchService(
            AlertRepository alertRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> searchAlerts(String platform, String symbol, String conditionTypeStr) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Alert search failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Alert search failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Parse condition type if provided
            Alert.ConditionType conditionType = null;
            if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
                try {
                    conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
                } catch (IllegalArgumentException e) {
                    result.put("success", false);
                    result.put("message", "Invalid condition type");
                    loggingService.logAction(userId, userIdStr, "Alert search failed: Invalid condition type: " + conditionTypeStr);
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
                loggingService.logAction(userId, userIdStr,
                        "Searching alerts with platform=" + platform + ", symbol=" + symbol + ", conditionType=" + conditionType);
            } else if (platform != null && !platform.trim().isEmpty() &&
                    symbol != null && !symbol.trim().isEmpty()) {
                // Search by platform and symbol
                alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndPlatformStock_StockSymbolAndActiveTrue(
                        userId, platform, symbol);
                loggingService.logAction(userId, userIdStr,
                        "Searching alerts with platform=" + platform + " and symbol=" + symbol);
            } else if (platform != null && !platform.trim().isEmpty() &&
                    conditionType != null) {
                // Search by platform and condition type
                alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndConditionTypeAndActiveTrue(
                        userId, platform, conditionType);
                loggingService.logAction(userId, userIdStr,
                        "Searching alerts with platform=" + platform + " and conditionType=" + conditionType);
            } else if (symbol != null && !symbol.trim().isEmpty() &&
                    conditionType != null) {
                // Search by symbol and condition type
                alerts = alertRepository.findByUser_UserIdAndPlatformStock_StockSymbolAndConditionTypeAndActiveTrue(
                        userId, symbol, conditionType);
                loggingService.logAction(userId, userIdStr,
                        "Searching alerts with symbol=" + symbol + " and conditionType=" + conditionType);
            } else if (platform != null && !platform.trim().isEmpty()) {
                // Search by platform only
                alerts = alertRepository.findByUser_UserIdAndPlatformStock_PlatformNameAndActiveTrue(userId, platform);
                loggingService.logAction(userId, userIdStr, "Searching alerts with platform=" + platform);
            } else if (symbol != null && !symbol.trim().isEmpty()) {
                // Search by symbol only
                alerts = alertRepository.findByUser_UserIdAndPlatformStock_StockSymbolAndActiveTrue(userId, symbol);
                loggingService.logAction(userId, userIdStr, "Searching alerts with symbol=" + symbol);
            } else if (conditionType != null) {
                // Search by condition type only
                alerts = alertRepository.findByUser_UserIdAndConditionTypeAndActiveTrue(userId, conditionType);
                loggingService.logAction(userId, userIdStr, "Searching alerts with conditionType=" + conditionType);
            } else {
                // No filters, get all active alerts for the user
                alerts = alertRepository.findByUser_UserIdAndActiveTrue(userId);
                loggingService.logAction(userId, userIdStr, "Searching alerts with no filters");
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
            loggingService.logAction(userId, userIdStr,
                    "Alert search successful, found " + alertsList.size() + " alerts");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error searching alerts: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching alerts");
        }

        return result;
    }
}
