package ehe_server.service.intf.alert;

import ehe_server.dto.AlertResponse;
import ehe_server.entity.Alert;

import java.math.BigDecimal;

public interface AlertCreationServiceInterface {
    /**
     * Creates a new alert for the current user
     * @param userId The user that has initiated the service
     * @param platform The trading platform name
     * @param symbol The stock symbol
     * @param conditionType The condition type (e.g., "ABOVE", "BELOW")
     * @param thresholdValue The threshold value for the alert
     * @return Map containing success status and created alert details
     */
    AlertResponse createAlert(Integer userId, String platform, String symbol, Alert.ConditionType conditionType, BigDecimal thresholdValue);
}