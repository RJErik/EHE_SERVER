package ehe_server.service.intf.alert;

import ehe_server.dto.AlertResponse;

import java.util.List;

/**
 * Interface for alert retrieval operations
 */
public interface AlertRetrievalServiceInterface {
    List<AlertResponse> getAlerts(Integer userId);
}