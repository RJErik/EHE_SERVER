package com.example.ehe_server.controller;

import com.example.ehe_server.dto.AlertAddRequest;
import com.example.ehe_server.dto.AlertDeleteRequest;
import com.example.ehe_server.dto.AlertSearchRequest;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AlertController {

    private final AlertServiceInterface alertService;
    private final AlertSearchServiceInterface alertSearchService;

    public AlertController(
            AlertServiceInterface alertService,
            AlertSearchServiceInterface alertSearchService) {
        this.alertService = alertService;
        this.alertSearchService = alertSearchService;
    }

    /**
     * Endpoint to retrieve all alerts for the current user
     *
     * @return List of alerts and success status
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        // Call alert service
        Map<String, Object> responseBody = alertService.getAlerts();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to add a new alert for the user
     *
     * @param request Contains platform name, stock symbol, condition type, and threshold value
     * @return Success status and details of added alert
     */
    @PostMapping("/alerts/add")
    public ResponseEntity<Map<String, Object>> addAlert(@RequestBody AlertAddRequest request) {
        // Call alert service to add item
        Map<String, Object> responseBody = alertService.addAlert(
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getThresholdValue());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to remove an alert
     *
     * @param request Contains the alert ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/alerts/remove")
    public ResponseEntity<Map<String, Object>> removeAlert(@RequestBody AlertDeleteRequest request) {
        // Call alert service to remove item
        Map<String, Object> responseBody = alertService.removeAlert(request.getId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to search alerts by platform, symbol, and/or condition type
     *
     * @param request Contains optional search criteria
     * @return Filtered list of alerts and success status
     */
    @PostMapping("/alerts/search")
    public ResponseEntity<Map<String, Object>> searchAlerts(@RequestBody AlertSearchRequest request) {
        // Call alert search service
        Map<String, Object> responseBody = alertSearchService.searchAlerts(
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
