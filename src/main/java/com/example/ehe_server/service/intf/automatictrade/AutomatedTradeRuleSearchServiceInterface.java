package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AutomatedTradeRuleSearchServiceInterface {
    List<AutomatedTradeRuleResponse> searchAutomatedTradeRules(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionType,
            String actionType,
            String quantityType,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue);
}