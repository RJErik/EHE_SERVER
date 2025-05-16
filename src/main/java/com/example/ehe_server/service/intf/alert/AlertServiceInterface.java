package com.example.ehe_server.service.intf.alert;

import java.math.BigDecimal;
import java.util.Map;

public interface AlertServiceInterface {
    Map<String, Object> getAlerts();
    Map<String, Object> addAlert(String platform, String symbol, String conditionType, BigDecimal thresholdValue);
    Map<String, Object> removeAlert(Integer alertId);
}
