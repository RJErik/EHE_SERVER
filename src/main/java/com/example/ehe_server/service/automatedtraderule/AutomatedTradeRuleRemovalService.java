package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AutomatedTradeRuleRemovalService implements AutomatedTradeRuleRemovalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final UserRepository userRepository;

    public AutomatedTradeRuleRemovalService(AutomatedTradeRuleRepository automatedTradeRuleRepository,
                                            UserRepository userRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.remove",
            params = {"#automatedTradeRuleId"}
    )
    @Override
    public void removeAutomatedTradeRule(Integer userId, Integer automatedTradeRuleId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (automatedTradeRuleId == null) {
            throw new MissingAutomatedTradeRuleIdException();
        }

        // Existence check
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

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