package com.example.ehe_server.service.intf.alert;

import com.example.ehe_server.dto.AlertResponse;

import java.util.List;
import java.util.Map;

/**
 * Interface for alert retrieval operations
 */
public interface AlertRetrievalServiceInterface {
    /**
     * Retrieves all active alerts for the current user
     *  @param userId The ID of who makes this request
     * @return Map containing success status and list of alerts
     */
    List<AlertResponse> getAlerts(Integer userId);
}