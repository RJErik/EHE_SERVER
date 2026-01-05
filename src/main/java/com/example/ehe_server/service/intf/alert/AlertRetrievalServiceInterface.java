package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.AlertResponse;

import java.util.List;
import java.util.Map;

/**
 * Interface for alert retrieval operations
 */
public interface AlertRetrievalServiceInterface {
    List<AlertResponse> getAlerts(Integer userId);
}