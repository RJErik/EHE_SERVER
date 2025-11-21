package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.AutomatedTradeRuleNotFoundException;
import com.example.ehe_server.exception.custom.UnauthorizedRuleAccessException;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeRuleRemovalService implements AutomatedTradeRuleRemovalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;

    public AutomatedTradeRuleRemovalService(AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.remove",
            params = {"#automatedTradeRuleId"}
    )
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

        // Deactivate the rule (not delete)
        rule.setActive(false);
        automatedTradeRuleRepository.save(rule);
    }
}