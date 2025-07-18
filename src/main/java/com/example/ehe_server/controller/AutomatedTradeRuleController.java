package com.example.ehe_server.controller;

import com.example.ehe_server.dto.AutomatedTradeRuleCreateRequest;
import com.example.ehe_server.dto.AutomatedTradeRuleDeleteRequest;
import com.example.ehe_server.dto.AutomatedTradeRuleSearchRequest;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AutomatedTradeRuleController {

    private final AutomatedTradeRuleServiceInterface automatedTradeRuleService;
    private final AutomatedTradeRuleSearchServiceInterface automatedTradeRuleSearchService;

    public AutomatedTradeRuleController(
            AutomatedTradeRuleServiceInterface automatedTradeRuleService,
            AutomatedTradeRuleSearchServiceInterface automatedTradeRuleSearchService) {
        this.automatedTradeRuleService = automatedTradeRuleService;
        this.automatedTradeRuleSearchService = automatedTradeRuleSearchService;
    }

    /**
     * Endpoint to retrieve all active automated trade rules for the current user
     *
     * @return List of automated trade rules and success status
     */
    @GetMapping("/automated-trade-rules")
    public ResponseEntity<Map<String, Object>> getAutomatedTradeRules() {
        // Call automated trade rule service
        Map<String, Object> responseBody = automatedTradeRuleService.getAutomatedTradeRules();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to add a new automated trade rule for the user
     *
     * @param request Contains portfolio ID, platform name, stock symbol, condition type, action type, quantity type, quantity, and threshold value
     * @return Success status and details of added automated trade rule
     */
    @PostMapping("/automated-trade-rules/add")
    public ResponseEntity<Map<String, Object>> addAutomatedTradeRule(@RequestBody AutomatedTradeRuleCreateRequest request) {
        // Call automated trade rule service to add item
        Map<String, Object> responseBody = automatedTradeRuleService.createAutomatedTradeRule(
                request.getPortfolioId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getActionType(),
                request.getQuantityType(),
                request.getQuantity(),
                request.getThresholdValue());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to remove (deactivate) an automated trade rule
     *
     * @param request Contains the automated trade rule ID to deactivate
     * @return Success status and confirmation message
     */
    @DeleteMapping("/automated-trade-rules/remove")
    public ResponseEntity<Map<String, Object>> removeAutomatedTradeRule(@RequestBody AutomatedTradeRuleDeleteRequest request) {
        // Call automated trade rule service to remove item
        Map<String, Object> responseBody = automatedTradeRuleService.removeAutomatedTradeRule(request.getId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to search automated trade rules by various criteria
     *
     * @param request Contains optional search criteria (portfolio ID, platform, symbol, condition type, action type, quantity type, min/max threshold)
     * @return Filtered list of automated trade rules and success status
     */
    @PostMapping("/automated-trade-rules/search")
    public ResponseEntity<Map<String, Object>> searchAutomatedTradeRules(@RequestBody AutomatedTradeRuleSearchRequest request) {
        // Call automated trade rule search service
        Map<String, Object> responseBody = automatedTradeRuleSearchService.searchAutomatedTradeRules(
                request.getPortfolioId(),
                request.getPlatform(),
                request.getSymbol(),
                request.getConditionType(),
                request.getActionType(),
                request.getQuantityType(),
                request.getMinThresholdValue(),
                request.getMaxThresholdValue());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}