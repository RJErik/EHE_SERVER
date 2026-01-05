package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;

import java.math.BigDecimal;
import java.util.List;

public interface AutomatedTradeRuleSearchServiceInterface {
    List<AutomatedTradeRuleResponse> searchAutomatedTradeRules(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            AutomatedTradeRule.ConditionType conditionType,
            AutomatedTradeRule.ActionType actionType,
            AutomatedTradeRule.QuantityType quantityType,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue);
}