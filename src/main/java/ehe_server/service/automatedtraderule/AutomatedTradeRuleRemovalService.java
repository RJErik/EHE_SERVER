package ehe_server.service.automatedtraderule;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.AutomatedTradeRuleNotFoundException;
import ehe_server.exception.custom.UnauthorizedAutomatedTradeRuleAccessException;
import ehe_server.repository.AutomatedTradeRuleRepository;
import ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Existence check
        AutomatedTradeRule rule = automatedTradeRuleRepository.findById(automatedTradeRuleId)
                .orElseThrow(() -> new AutomatedTradeRuleNotFoundException(automatedTradeRuleId));

        // Authorization verification
        if (!rule.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAutomatedTradeRuleAccessException(userId, automatedTradeRuleId);
        }

        // Execute removal
        automatedTradeRuleRepository.delete(rule);
    }
}