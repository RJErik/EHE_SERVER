package com.example.ehe_server.service.intf.automatictrade;

import java.math.BigDecimal;
import java.util.Map;

public interface AutomatedTradeRuleServiceInterface {
    Map<String, Object> getAutomatedTradeRules();
    Map<String, Object> createAutomatedTradeRule(
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionType,
            String actionType,
            String quantityType,
            BigDecimal quantity,
            BigDecimal thresholdValue);
    Map<String, Object> removeAutomatedTradeRule(Integer automatedTradeRuleId);
}