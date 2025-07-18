package com.example.ehe_server.service.alert;

import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.alert.AlertServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertService implements AlertServiceInterface {

    private final AlertRepository alertRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public AlertService(
            AlertRepository alertRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.alertRepository = alertRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getAlerts() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Get all active alerts for the user
            List<Alert> alerts = alertRepository.findByUser_UserIdAndActiveTrue(userId);

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
            loggingService.logAction("Alerts retrieved successfully");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving alerts: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving alerts");
        }

        return result;
    }

    @Override
    public Map<String, Object> addAlert(String platform, String symbol, String conditionTypeStr, BigDecimal thresholdValue) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate threshold value
            if (thresholdValue == null || thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
                result.put("success", false);
                result.put("message", "Threshold value must be greater than zero");
                loggingService.logAction("Alert add failed: Invalid threshold value");
                return result;
            }

            // Parse condition type
            Alert.ConditionType conditionType;
            try {
                conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "Invalid condition type");
                loggingService.logAction("Alert add failed: Invalid condition type: " + conditionTypeStr);
                return result;
            }

            // Check if platform and symbol combination exists
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
            if (platformStocks.isEmpty()) {
                result.put("success", false);
                result.put("message", "Platform and symbol combination does not exist");
                loggingService.logAction("Alert add failed: Platform/symbol combination not found: " + platform + "/" + symbol);
                return result;
            }

            PlatformStock platformStock = platformStocks.get(0);

            // Create new alert
            Alert newAlert = new Alert();
            newAlert.setUser(userContextService.getCurrentHumanUser());
            newAlert.setPlatformStock(platformStock);
            newAlert.setConditionType(conditionType);
            newAlert.setThresholdValue(thresholdValue);
            newAlert.setDateCreated(LocalDateTime.now());
            newAlert.setActive(true);
            Alert savedAlert = alertRepository.save(newAlert);

            // Prepare success response
            Map<String, Object> alertMap = new HashMap<>();
            alertMap.put("id", savedAlert.getAlertId());
            alertMap.put("platform", platform);
            alertMap.put("symbol", symbol);
            alertMap.put("conditionType", savedAlert.getConditionType().toString());
            alertMap.put("thresholdValue", savedAlert.getThresholdValue());
            alertMap.put("dateCreated", savedAlert.getDateCreated().format(DATE_FORMATTER));
            alertMap.put("isActive", savedAlert.isActive());

            result.put("success", true);
            result.put("message", "Alert created successfully");
            result.put("alert", alertMap);

            // Log success
            loggingService.logAction("Added alert: " + platform + "/" + symbol + " " + conditionType + " " + thresholdValue);

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error adding alert: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while creating alert");
        }

        return result;
    }

    @Override
    public Map<String, Object> removeAlert(Integer alertId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if the alert exists
            Optional<Alert> alertOptional = alertRepository.findById(alertId);
            if (alertOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Alert not found");
                loggingService.logAction("Alert remove failed: Alert not found with ID: " + alertId);
                return result;
            }

            Alert alert = alertOptional.get();

            // Verify the alert belongs to the user
            if (!alert.getUser().getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "Not authorized to remove this alert");
                loggingService.logAction("Alert remove failed: Unauthorized access to alert ID: " + alertId);
                return result;
            }

            // Get alert details for logging
            String platform = alert.getPlatformStock().getPlatformName();
            String symbol = alert.getPlatformStock().getStockSymbol();
            Alert.ConditionType conditionType = alert.getConditionType();
            BigDecimal thresholdValue = alert.getThresholdValue();

            // Remove the alert
            alertRepository.delete(alert);

            // Prepare success response
            result.put("success", true);
            result.put("message", "Alert removed successfully");

            // Log success
            loggingService.logAction("Removed alert: " + platform + "/" + symbol + " " + conditionType + " " + thresholdValue + " (ID: " + alertId + ")");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error removing alert: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while removing alert");
        }

        return result;
    }
}
