package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.AutomatedTradeRule;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleSearchServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AutomatedTradeRuleSearchService implements AutomatedTradeRuleSearchServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleSearchService(AutomatedTradeRuleRepository automatedTradeRuleRepository,
                                           UserRepository userRepository,
                                           PortfolioRepository portfolioRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.search",
            params = {
                    "#portfolioId",
                    "#platform",
                    "#symbol",
                    "#conditionTypeStr",
                    "#actionTypeStr",
                    "#quantityTypeStr",
                    "#result.size()"}
    )
    @Override
    public List<AutomatedTradeRuleResponse> searchAutomatedTradeRules(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionTypeStr,
            String actionTypeStr,
            String quantityTypeStr,
            BigDecimal minThresholdValue,
            BigDecimal maxThresholdValue) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Parsing logic
        AutomatedTradeRule.ConditionType conditionType = null;
        if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
            try {
                conditionType = AutomatedTradeRule.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidConditionTypeException(conditionTypeStr);
            }
        }

        AutomatedTradeRule.ActionType actionType = null;
        if (actionTypeStr != null && !actionTypeStr.trim().isEmpty()) {
            try {
                actionType = AutomatedTradeRule.ActionType.valueOf(actionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidActionTypeException(actionTypeStr);
            }
        }

        AutomatedTradeRule.QuantityType quantityType = null;
        if (quantityTypeStr != null && !quantityTypeStr.trim().isEmpty()) {
            try {
                quantityType = AutomatedTradeRule.QuantityType.valueOf(quantityTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidQuantityTypeException(quantityTypeStr);
            }
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        if (portfolioId != null) {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

            if (!portfolio.getUser().getUserId().equals(userId)) {
                throw new UnauthorizedPortfolioAccessException(userId, portfolioId);
            }
        }

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
                .map(rule -> {
                    AutomatedTradeRuleResponse dto = new AutomatedTradeRuleResponse();
                    dto.setId(rule.getAutomatedTradeRuleId());
                    dto.setPortfolioId(rule.getPortfolio().getPortfolioId());
                    dto.setPortfolioName(rule.getPortfolio().getPortfolioName());
                    dto.setPlatform(rule.getPlatformStock().getPlatform().getPlatformName());
                    dto.setSymbol(rule.getPlatformStock().getStock().getStockName());
                    dto.setConditionType(rule.getConditionType().toString());
                    dto.setActionType(rule.getActionType().toString());
                    dto.setQuantityType(rule.getQuantityType().toString());
                    dto.setQuantity(rule.getQuantity());
                    dto.setThresholdValue(rule.getThresholdValue());
                    dto.setDateCreated(rule.getDateCreated().format(DATE_FORMATTER));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}