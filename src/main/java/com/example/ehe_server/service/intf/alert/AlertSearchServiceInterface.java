package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.AlertSearchResponse;

import java.util.List;

public interface AlertSearchServiceInterface {
    List<AlertSearchResponse> searchAlerts(Integer userId, String platform, String symbol, String conditionType);
}
