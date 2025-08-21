package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleSearchResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AutomatedTradeRuleSearchServiceInterface {
    List<AutomatedTradeRuleSearchResponse> searchAutomatedTradeRules(
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