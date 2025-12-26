package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.AlertResponse;

import java.util.List;

public interface AlertSearchServiceInterface {
    List<AlertResponse> searchAlerts(Integer userId, String platform, String symbol, String conditionType);
}
