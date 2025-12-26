package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertCreationServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/alerts")
public class AlertController {

    private final AlertRetrievalServiceInterface alertRetrievalService;
    private final AlertCreationServiceInterface alertCreationService;
    private final AlertRemovalServiceInterface alertRemovalService;
    private final AlertSearchServiceInterface alertSearchService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;

    public AlertController(
            AlertRetrievalServiceInterface alertRetrievalService,
            AlertCreationServiceInterface alertCreationService,
            AlertRemovalServiceInterface alertRemovalService,
            AlertSearchServiceInterface alertSearchService,
            UserContextService userContextService,
            MessageSource messageSource) {
        this.alertRetrievalService = alertRetrievalService;
        this.alertCreationService = alertCreationService;
        this.alertRemovalService = alertRemovalService;
        this.alertSearchService = alertSearchService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/alerts
     * Retrieve all alerts for the current user
     *
     * @return List of alerts and success status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts() {
        List<AlertResponse> alertRetrievalResponses =
                alertRetrievalService.getAlerts(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.alert.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alerts", alertRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/alerts
     * Create a new alert for the user
     *
     * @param request Contains platform name, stock symbol, condition type, and threshold value
     * @return Success status and details of added alert
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAlert(@RequestBody AlertCreationRequest request) {
        AlertResponse alertCreationResponse = alertCreationService.createAlert(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getThresholdValue());

        String successMessage = messageSource.getMessage(
                "success.message.alert.add",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alert", alertCreationResponse);

        // Return 201 Created for resource creation
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * DELETE /api/user/alerts/{alertId}
     * Remove an alert by ID
     *
     * @param alertId The alert ID to delete (from path)
     * @return Success status and confirmation message
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<Map<String, Object>> removeAlert(@PathVariable Integer alertId) {
        alertRemovalService.removeAlert(userContextService.getCurrentUserId(), alertId);

        String successMessage = messageSource.getMessage(
                "success.message.alert.remove",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/alerts/search?platform=X&symbol=Y&conditionType=Z
     * Search alerts by platform, symbol, and/or condition type
     *
     * @param request Contains optional search criteria (bound from query params)
     * @return Filtered list of alerts and success status
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAlerts(@ModelAttribute AlertSearchRequest request) {
        List<AlertResponse> alertSearchResponses = alertSearchService.searchAlerts(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType());

        String successMessage = messageSource.getMessage(
                "success.message.alert.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alerts", alertSearchResponses);

        return ResponseEntity.ok(responseBody);
    }
}