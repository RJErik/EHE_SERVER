package com.example.ehe_server.service.intf.alert;

import java.util.Map;

public interface AlertSearchServiceInterface {
    Map<String, Object> searchAlerts(String platform, String symbol, String conditionType);
}
