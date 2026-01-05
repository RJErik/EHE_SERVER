package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AutomatedTradeRuleRetrievalService implements AutomatedTradeRuleRetrievalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;

    public AutomatedTradeRuleRetrievalService(AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AutomatedTradeRuleResponse> getAutomatedTradeRules(Integer userId) {

        // Data retrieval
        List<AutomatedTradeRule> rules = automatedTradeRuleRepository.findByUser_UserIdOrderByDateCreatedDesc(userId);

        // Response mapping
        return rules.stream()
                .map(rule -> new AutomatedTradeRuleResponse(
                        rule.getAutomatedTradeRuleId(),
                        rule.getPortfolio().getPortfolioId(),
                        rule.getPortfolio().getPortfolioName(),
                        rule.getPlatformStock().getPlatform().getPlatformName(),
                        rule.getPlatformStock().getStock().getStockSymbol(),
                        rule.getConditionType(),
                        rule.getActionType(),
                        rule.getQuantityType(),
                        rule.getQuantity(),
                        rule.getThresholdValue(),
                        rule.getDateCreated()
                ))
                .collect(Collectors.toList());
    }
}