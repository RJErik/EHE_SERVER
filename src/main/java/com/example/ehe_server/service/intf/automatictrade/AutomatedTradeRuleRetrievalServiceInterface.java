package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleResponse;

import java.util.List;

/**
 * Interface for automated trade rule retrieval operations
 */
public interface AutomatedTradeRuleRetrievalServiceInterface {
    List<AutomatedTradeRuleResponse> getAutomatedTradeRules(Integer userId);
}