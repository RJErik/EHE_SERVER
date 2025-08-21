package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.AutomatedTradeRuleNotFoundException;
import com.example.ehe_server.exception.custom.UnauthorizedRuleAccessException;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeRuleRemovalService implements AutomatedTradeRuleRemovalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final LoggingServiceInterface loggingService;

    public AutomatedTradeRuleRemovalService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            LoggingServiceInterface loggingService) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.loggingService = loggingService;
    }

    @Override
    public void removeAutomatedTradeRule(Integer userId, Integer automatedTradeRuleId) {
        // Check if the rule exists
        Optional<AutomatedTradeRule> ruleOptional = automatedTradeRuleRepository.findById(automatedTradeRuleId);
        if (ruleOptional.isEmpty()) {
            throw new AutomatedTradeRuleNotFoundException(automatedTradeRuleId);
        }

        AutomatedTradeRule rule = ruleOptional.get();

        // Verify the rule belongs to the user
        if (!rule.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedRuleAccessException(automatedTradeRuleId, userId);
        }

        // Get rule details for logging
        String platform = rule.getPlatformStock().getPlatformName();
        String symbol = rule.getPlatformStock().getStockSymbol();
        AutomatedTradeRule.ConditionType conditionType = rule.getConditionType();
        AutomatedTradeRule.ActionType actionType = rule.getActionType();
        BigDecimal thresholdValue = rule.getThresholdValue();

        // Deactivate the rule (not delete)
        rule.setActive(false);
        automatedTradeRuleRepository.save(rule);

        // Log success
        loggingService.logAction("Deactivated automated trade rule: " + platform + "/" + symbol + " " +
                conditionType + " " + thresholdValue + " -> " + actionType +
                " (ID: " + automatedTradeRuleId + ")");
    }
}