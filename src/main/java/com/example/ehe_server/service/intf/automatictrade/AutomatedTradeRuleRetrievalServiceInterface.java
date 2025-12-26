package com.example.ehe_server.service.intf.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleResponse;

import java.util.List;

/**
 * Interface for automated trade rule retrieval operations
 */
public interface AutomatedTradeRuleRetrievalServiceInterface {
    /**
     * Retrieves all active automated trade rules for the current user
     * @param userId The user that has initiated the service
     * @return Map containing success status and list of automated trade rules
     */
    List<AutomatedTradeRuleResponse> getAutomatedTradeRules(Integer userId);
}