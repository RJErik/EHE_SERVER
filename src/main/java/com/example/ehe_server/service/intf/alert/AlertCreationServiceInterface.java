package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.AlertCreationResponse;

import java.math.BigDecimal;

public interface AlertCreationServiceInterface {
    /**
     * Creates a new alert for the current user
     * @param userId The user that has initiated the service
     * @param platform The trading platform name
     * @param symbol The stock symbol
     * @param conditionTypeStr The condition type (e.g., "ABOVE", "BELOW")
     * @param thresholdValue The threshold value for the alert
     * @return Map containing success status and created alert details
     */
    AlertCreationResponse createAlert(Integer userId, String platform, String symbol, String conditionTypeStr, BigDecimal thresholdValue);
}