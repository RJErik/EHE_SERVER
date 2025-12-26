package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AutomatedTradeRuleRetrievalService implements AutomatedTradeRuleRetrievalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleRetrievalService(AutomatedTradeRuleRepository automatedTradeRuleRepository,
                                              UserRepository userRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AutomatedTradeRuleResponse> getAutomatedTradeRules(Integer userId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval and response mapping
        List<AutomatedTradeRule> rules = automatedTradeRuleRepository.findByUser_UserIdOrderByDateCreatedDesc(userId);

        return rules.stream()
                .map(rule -> new AutomatedTradeRuleResponse(
                        rule.getAutomatedTradeRuleId(),
                        rule.getPortfolio().getPortfolioId(),
                        rule.getPortfolio().getPortfolioName(),
                        rule.getPlatformStock().getPlatform().getPlatformName(),
                        rule.getPlatformStock().getStock().getStockName(),
                        rule.getConditionType().toString(),
                        rule.getActionType().toString(),
                        rule.getQuantityType().toString(),
                        rule.getQuantity(),
                        rule.getThresholdValue(),
                        rule.getDateCreated().format(DATE_FORMATTER)
                ))
                .collect(Collectors.toList());
    }
}