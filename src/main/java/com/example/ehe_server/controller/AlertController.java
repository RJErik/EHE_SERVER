package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertRetrievalServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertCreationServiceInterface;
import com.example.ehe_server.service.intf.alert.AlertRemovalServiceInterface;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
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
     * Endpoint to retrieve all alerts for the current user
     *
     * @return List of alerts and success status
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        // Call alert retrieval service
        List<AlertRetrievalResponse> alertRetrievalResponses = alertRetrievalService.getAlerts(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.alert.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alerts", alertRetrievalResponses); // Nest the DTO under a "data" key

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to add a new alert for the user
     *
     * @param request Contains platform name, stock symbol, condition type, and threshold value
     * @return Success status and details of added alert
     */
    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> createAlert(@RequestBody AlertCreationRequest request) {
        // Call alert creation service to add item
        AlertCreationResponse alertCreationResponse = alertCreationService.createAlert(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getThresholdValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.alert.add", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alert", alertCreationResponse); // Nest the DTO under a "data" key

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to remove an alert
     *
     * @param request Contains the alert ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/alerts")
    public ResponseEntity<Map<String, Object>> removeAlert(@RequestBody AlertRemovalRequest request) {
        // Call alert removal service to remove item
        alertRemovalService.removeAlert(userContextService.getCurrentUserId(), request.getId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.alert.remove", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
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
        List<AlertSearchResponse> alertSearchResponses = alertSearchService.searchAlerts(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.alert.search", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("alerts", alertSearchResponses); // Nest the DTO under a "data" key

        return ResponseEntity.ok(responseBody);
    }
}