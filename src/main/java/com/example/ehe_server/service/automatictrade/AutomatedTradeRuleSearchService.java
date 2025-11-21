package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleSearchResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.InvalidActionTypeException;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.exception.custom.InvalidQuantityTypeException;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleSearchService(AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.search",
            params = {
                    "#portfolioId",
                    "#platform",
                    "#symbol",
                    "#conditionTypeStr",
                    "#actionTypeStr",
                    "#quantityTypeStr",
                    "#result.size()"}
    )
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

        // Execute single database query with all filters
        List<AutomatedTradeRule> filteredRules = automatedTradeRuleRepository.searchAutomatedTradeRules(
                userId,
                portfolioId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null,
                conditionType,
                actionType,
                quantityType,
                minThresholdValue,
                maxThresholdValue
        );

        // Transform to response format
        return filteredRules.stream()
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
    }
}