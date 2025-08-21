package com.example.ehe_server.service.intf.alert;

public interface AlertRemovalServiceInterface {
    /**
     * Removes an alert belonging to the current user
     * @param userId The ID of who makes this request
     * @param alertId The ID of the alert to remove
     * @return Map containing success status and removal confirmation
     */
    void removeAlert(Integer userId, Integer alertId);
}