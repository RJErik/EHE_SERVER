package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleRetrievalResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutomatedTradeRuleRetrievalService implements AutomatedTradeRuleRetrievalServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleRetrievalService(AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.get",
            params = {"#result.size()"}
    )
    @Override
    public List<AutomatedTradeRuleRetrievalResponse> getAutomatedTradeRules(Integer userId) {

        // Get all active automated trade rules for the user
        List<AutomatedTradeRule> rules = automatedTradeRuleRepository.findByUser_UserIdAndIsActiveTrue(userId);

        // Transform to response format
        return rules.stream()
                .map(rule -> new AutomatedTradeRuleRetrievalResponse(
                        rule.getAutomatedTradeRuleId(),
                        rule.getPortfolio().getPortfolioId(),
                        rule.getPortfolio().getPortfolioName(),
                        rule.getPlatformStock().getPlatformName(),
                        rule.getPlatformStock().getStockSymbol(),
                        rule.getConditionType().toString(),
                        rule.getActionType().toString(),
                        rule.getQuantityType().toString(),
                        rule.getQuantity(),
                        rule.getThresholdValue(),
                        rule.getDateCreated().format(DATE_FORMATTER),
                        rule.isActive()
                ))
                .collect(Collectors.toList());
    }
}