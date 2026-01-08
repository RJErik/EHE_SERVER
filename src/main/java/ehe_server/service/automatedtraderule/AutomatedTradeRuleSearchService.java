package ehe_server.service.automatedtraderule;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.AutomatedTradeRuleResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.repository.AutomatedTradeRuleRepository;
import ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AutomatedTradeRuleSearchService implements AutomatedTradeRuleSearchServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;

    public AutomatedTradeRuleSearchService(AutomatedTradeRuleRepository automatedTradeRuleRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.search",
            params = {
                    "#portfolioId",
                    "#platform",
                    "#symbol",
                    "#conditionType",
                    "#actionType",
                    "#quantityType",
                    "#minThresholdValue",
                    "#maxThresholdValue",
                    "#result.size()"}
    )
    @Override
    public List<AutomatedTradeRuleResponse> searchAutomatedTradeRules(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            AutomatedTradeRule.ConditionType conditionType,
            AutomatedTradeRule.ActionType actionType,
            AutomatedTradeRule.QuantityType quantityType,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue) {

        // Data retrieval
        List<AutomatedTradeRule> filteredRules = automatedTradeRuleRepository.searchAutomatedTradeRules(
                userId,
                portfolioId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null,
                conditionType,
                actionType,
                quantityType,
                minThresholdValue,
                maxThresholdValue
        );


        // Response mapping
        return filteredRules.stream()
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