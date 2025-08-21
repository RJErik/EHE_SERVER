package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleSearchResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.InvalidActionTypeException;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.exception.custom.InvalidQuantityTypeException;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutomatedTradeRuleSearchService implements AutomatedTradeRuleSearchServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleSearchService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            LoggingServiceInterface loggingService) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.loggingService = loggingService;
    }

    @Override
    public List<AutomatedTradeRuleSearchResponse> searchAutomatedTradeRules(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionTypeStr,
            String actionTypeStr,
            String quantityTypeStr,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue) {

        // Parse condition type if provided
        AutomatedTradeRule.ConditionType conditionType = null;
        if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
            try {
                conditionType = AutomatedTradeRule.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidConditionTypeException(conditionTypeStr);
            }
        }

        // Parse action type if provided
        AutomatedTradeRule.ActionType actionType = null;
        if (actionTypeStr != null && !actionTypeStr.trim().isEmpty()) {
            try {
                actionType = AutomatedTradeRule.ActionType.valueOf(actionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidActionTypeException(actionTypeStr);
            }
        }

        // Parse quantity type if provided
        AutomatedTradeRule.QuantityType quantityType = null;
        if (quantityTypeStr != null && !quantityTypeStr.trim().isEmpty()) {
            try {
                quantityType = AutomatedTradeRule.QuantityType.valueOf(quantityTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidQuantityTypeException(quantityTypeStr);
            }
        }

        // Start with all active rules for this user
        List<AutomatedTradeRule> allRules = automatedTradeRuleRepository.findByUser_UserIdAndIsActiveTrue(userId);

        // Create final copies of the filter parameters to use in lambda expressions
        final Integer finalPortfolioId = portfolioId;
        final String finalPlatform = platform;
        final String finalSymbol = symbol;
        final AutomatedTradeRule.ConditionType finalConditionType = conditionType;
        final AutomatedTradeRule.ActionType finalActionType = actionType;
        final AutomatedTradeRule.QuantityType finalQuantityType = quantityType;
        final BigDecimal finalMinThresholdValue = minThresholdValue;
        final BigDecimal finalMaxThresholdValue = maxThresholdValue;

        // Apply filters
        List<AutomatedTradeRule> filteredRules = allRules.stream()
                .filter(rule -> finalPortfolioId == null || rule.getPortfolio().getPortfolioId().equals(finalPortfolioId))
                .filter(rule -> finalPlatform == null || finalPlatform.trim().isEmpty() ||
                        rule.getPlatformStock().getPlatformName().equals(finalPlatform))
                .filter(rule -> finalSymbol == null || finalSymbol.trim().isEmpty() ||
                        rule.getPlatformStock().getStockSymbol().equals(finalSymbol))
                .filter(rule -> finalConditionType == null || rule.getConditionType() == finalConditionType)
                .filter(rule -> finalActionType == null || rule.getActionType() == finalActionType)
                .filter(rule -> finalQuantityType == null || rule.getQuantityType() == finalQuantityType)
                .filter(rule -> finalMinThresholdValue == null ||
                        rule.getThresholdValue().compareTo(finalMinThresholdValue) >= 0)
                .filter(rule -> finalMaxThresholdValue == null ||
                        rule.getThresholdValue().compareTo(finalMaxThresholdValue) <= 0)
                .toList();

        // Log the search parameters
        StringBuilder searchParams = new StringBuilder("Searching automated trade rules with filters: ");
        if (portfolioId != null) searchParams.append("portfolioId=").append(portfolioId).append(", ");
        if (platform != null && !platform.trim().isEmpty())
            searchParams.append("platform=").append(platform).append(", ");
        if (symbol != null && !symbol.trim().isEmpty()) searchParams.append("symbol=").append(symbol).append(", ");
        if (conditionType != null) searchParams.append("conditionType=").append(conditionType).append(", ");
        if (actionType != null) searchParams.append("actionType=").append(actionType).append(", ");
        if (quantityType != null) searchParams.append("quantityType=").append(quantityType).append(", ");
        if (minThresholdValue != null) searchParams.append("minThreshold=").append(minThresholdValue).append(", ");
        if (maxThresholdValue != null) searchParams.append("maxThreshold=").append(maxThresholdValue).append(", ");

        String logMessage = searchParams.toString();
        if (logMessage.endsWith(", ")) {
            logMessage = logMessage.substring(0, logMessage.length() - 2);
        }

        loggingService.logAction(logMessage);

        // Transform to response format
        List<AutomatedTradeRuleSearchResponse> rulesList = filteredRules.stream()
                .map(rule -> {
                    AutomatedTradeRuleSearchResponse dto = new AutomatedTradeRuleSearchResponse();
                    dto.setId(rule.getAutomatedTradeRuleId());
                    dto.setPortfolioId(rule.getPortfolio().getPortfolioId());
                    dto.setPortfolioName(rule.getPortfolio().getPortfolioName());
                    dto.setPlatform(rule.getPlatformStock().getPlatformName());
                    dto.setSymbol(rule.getPlatformStock().getStockSymbol());
                    dto.setConditionType(rule.getConditionType().toString());
                    dto.setActionType(rule.getActionType().toString());
                    dto.setQuantityType(rule.getQuantityType().toString());
                    dto.setQuantity(rule.getQuantity());
                    dto.setThresholdValue(rule.getThresholdValue());
                    dto.setDateCreated(rule.getDateCreated().format(DATE_FORMATTER));
                    dto.setActive(rule.isActive());
                    return dto;
                })
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Automated trade rule search successful, found " + rulesList.size() + " rules");

        return rulesList;
    }
}