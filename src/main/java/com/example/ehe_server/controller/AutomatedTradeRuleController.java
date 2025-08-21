package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRetrievalServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleCreationServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AutomatedTradeRuleController {

    private final AutomatedTradeRuleRetrievalServiceInterface automatedTradeRuleRetrievalService;
    private final AutomatedTradeRuleCreationServiceInterface automatedTradeRuleCreationService;
    private final AutomatedTradeRuleRemovalServiceInterface automatedTradeRuleRemovalService;
    private final AutomatedTradeRuleSearchServiceInterface automatedTradeRuleSearchService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;

    public AutomatedTradeRuleController(
            AutomatedTradeRuleRetrievalServiceInterface automatedTradeRuleRetrievalService,
            AutomatedTradeRuleCreationServiceInterface automatedTradeRuleCreationService,
            AutomatedTradeRuleRemovalServiceInterface automatedTradeRuleRemovalService,
            AutomatedTradeRuleSearchServiceInterface automatedTradeRuleSearchService,
            UserContextService userContextService, MessageSource messageSource) {
        this.automatedTradeRuleRetrievalService = automatedTradeRuleRetrievalService;
        this.automatedTradeRuleCreationService = automatedTradeRuleCreationService;
        this.automatedTradeRuleRemovalService = automatedTradeRuleRemovalService;
        this.automatedTradeRuleSearchService = automatedTradeRuleSearchService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    /**
     * Endpoint to retrieve all active automated trade rules for the current user
     *
     * @return List of automated trade rules and success status
     */
    @GetMapping("/automated-trade-rules")
    public ResponseEntity<Map<String, Object>> getAutomatedTradeRules() {
        // Call automated trade rule retrieval service
        List<AutomatedTradeRuleRetrievalResponse> automatedTradeRuleRetrievalResponses = automatedTradeRuleRetrievalService.getAutomatedTradeRules(userContextService.getCurrentUserId().intValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRules", automatedTradeRuleRetrievalResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OUT NAME

    /**
     * Endpoint to add a new automated trade rule for the user
     *
     * @param request Contains portfolio ID, platform name, stock symbol, condition type, action type, quantity type, quantity, and threshold value
     * @return Success status and details of added automated trade rule
     */
    @PostMapping("/automated-trade-rules")
    public ResponseEntity<Map<String, Object>> createAutomatedTradeRule(@RequestBody AutomatedTradeRuleCreationRequest request) {
        // Call alert retrieval service
        AutomatedTradeRuleCreationResponse automatedTradeRuleCreationResponse = automatedTradeRuleCreationService.createAutomatedTradeRule(
                userContextService.getCurrentUserId().intValue(),
                request.getPortfolioId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getActionType(),
                request.getQuantityType(),
                request.getQuantity(),
                request.getThresholdValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.add", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRule", automatedTradeRuleCreationResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to remove (deactivate) an automated trade rule
     *
     * @param request Contains the automated trade rule ID to deactivate
     * @return Success status and confirmation message
     */
    @DeleteMapping("/automated-trade-rules")
    public ResponseEntity<Map<String, Object>> removeAutomatedTradeRule(@RequestBody AutomatedTradeRemovalRequest request) {
        // Call alert retrieval service
        automatedTradeRuleRemovalService.removeAutomatedTradeRule(userContextService.getCurrentUserId().intValue(), request.getId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.remove", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to search automated trade rules by various criteria
     *
     * @param request Contains optional search criteria (portfolio ID, platform, symbol, condition type, action type, quantity type, min/max threshold)
     * @return Filtered list of automated trade rules and success status
     */
    @PostMapping("/automated-trade-rules/search")
    public ResponseEntity<Map<String, Object>> searchAutomatedTradeRules(@RequestBody AutomatedTradeRuleSearchRequest request) {
        // Call automated trade rule retrieval service
        List<AutomatedTradeRuleSearchResponse> automatedTradeRuleSearchResponses = automatedTradeRuleSearchService.searchAutomatedTradeRules(
                userContextService.getCurrentUserId().intValue(),
                request.getPortfolioId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getActionType(),
                request.getQuantityType(),
                request.getMinThresholdValue(),
                request.getMaxThresholdValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.search", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRules", automatedTradeRuleSearchResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}