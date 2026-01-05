package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;

import java.math.BigDecimal;

public interface AutomatedTradeRuleCreationServiceInterface {
    /**
     * Creates a new automated trade rule for the current user
     * @param userId The user that has initiated the service
     * @param portfolioId The portfolio ID to associate with the rule
     * @param platform The trading platform name
     * @param symbol The stock symbol
     * @param conditionType The condition type (e.g., "ABOVE", "BELOW")
     * @param actionType The action type (e.g., "BUY", "SELL")
     * @param quantityType The quantity type (e.g., "SHARES", "PERCENTAGE")
     * @param quantity The quantity amount
     * @param thresholdValue The threshold value for the condition
     * @return Map containing success status and created rule details
     */
    AutomatedTradeRuleResponse createAutomatedTradeRule(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            AutomatedTradeRule.ConditionType conditionType,
            AutomatedTradeRule.ActionType actionType,
            AutomatedTradeRule.QuantityType quantityType,
            BigDecimal quantity,
            BigDecimal thresholdValue);
}
