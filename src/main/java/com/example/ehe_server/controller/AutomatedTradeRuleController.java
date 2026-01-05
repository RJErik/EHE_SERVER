package com.example.ehe_server.controller;

import com.example.ehe_server.annotation.validation.NotNullField;
import com.example.ehe_server.dto.*;
import com.example.ehe_server.exception.custom.MissingAutomatedTradeRuleIdException;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRetrievalServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleCreationServiceInterface;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/automated-trade-rules")
@Validated
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
            UserContextService userContextService,
            MessageSource messageSource) {
        this.automatedTradeRuleRetrievalService = automatedTradeRuleRetrievalService;
        this.automatedTradeRuleCreationService = automatedTradeRuleCreationService;
        this.automatedTradeRuleRemovalService = automatedTradeRuleRemovalService;
        this.automatedTradeRuleSearchService = automatedTradeRuleSearchService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/automated-trade-rules
     * Retrieve all active automated trade rules for the current user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAutomatedTradeRules() {
        List<AutomatedTradeRuleResponse> automatedTradeRuleRetrievalResponses =
                automatedTradeRuleRetrievalService.getAutomatedTradeRules(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRules", automatedTradeRuleRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/automated-trade-rules
     * Create a new automated trade rule
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAutomatedTradeRule(
            @Valid @RequestBody AutomatedTradeRuleCreationRequest request) {

        AutomatedTradeRuleResponse automatedTradeRuleCreationResponse =
                automatedTradeRuleCreationService.createAutomatedTradeRule(
                        userContextService.getCurrentUserId(),
                        request.getPortfolioId(),
                        request.getPlatform(),
                        request.getSymbol(),
                        request.getConditionType(),
                        request.getActionType(),
                        request.getQuantityType(),
                        request.getQuantity(),
                        request.getThresholdValue());

        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.add",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRule", automatedTradeRuleCreationResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * DELETE /api/user/automated-trade-rules/{ruleId}
     * Remove (deactivate) an automated trade rule
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> removeAutomatedTradeRule(
            @NotNullField(exception = MissingAutomatedTradeRuleIdException.class)
            @PathVariable
            Integer ruleId) {
        automatedTradeRuleRemovalService.removeAutomatedTradeRule(
                userContextService.getCurrentUserId(),
                ruleId);

        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.remove",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/automated-trade-rules/search?portfolioId=1&platform=BINANCE&symbol=BTC...
     * Search automated trade rules by various criteria
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAutomatedTradeRules(
            @ModelAttribute AutomatedTradeRuleSearchRequest request) {

        List<AutomatedTradeRuleResponse> automatedTradeRuleSearchResponses =
                automatedTradeRuleSearchService.searchAutomatedTradeRules(
                        userContextService.getCurrentUserId(),
                        request.getPortfolioId(),
                        request.getPlatform(),
                        request.getSymbol(),
                        request.getConditionType(),
                        request.getActionType(),
                        request.getQuantityType(),
                        request.getMinThresholdValue(),
                        request.getMaxThresholdValue());

        String successMessage = messageSource.getMessage(
                "success.message.automatedTradeRule.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("automatedTradeRules", automatedTradeRuleSearchResponses);

        return ResponseEntity.ok(responseBody);
    }
}