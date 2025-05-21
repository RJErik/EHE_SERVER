package com.example.ehe_server.service.intf.automatictrade;

import java.math.BigDecimal;
import java.util.Map;

public interface AutomatedTradeRuleSearchServiceInterface {
    Map<String, Object> searchAutomatedTradeRules(
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionType,
            String actionType,
            String quantityType,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue);
}