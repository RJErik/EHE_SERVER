package com.example.ehe_server.service.intf.automatictrade;

public interface AutomatedTradeRuleRemovalServiceInterface {
    /**
     * Deactivates an automated trade rule belonging to the current user
     * @param userId The user that has initiated the service
     * @param automatedTradeRuleId The ID of the rule to deactivate
     * @return Map containing success status and deactivation confirmation
     */
    void removeAutomatedTradeRule(Integer userId, Integer automatedTradeRuleId);
}
