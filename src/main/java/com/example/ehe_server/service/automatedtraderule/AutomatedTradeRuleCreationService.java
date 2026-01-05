package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeRuleCreationService implements AutomatedTradeRuleCreationServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlatformStockRepository platformStockRepository;

    public AutomatedTradeRuleCreationService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            PortfolioRepository portfolioRepository,
            PlatformStockRepository platformStockRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.portfolioRepository = portfolioRepository;
        this.platformStockRepository = platformStockRepository;
    }

    @LogMessage(
            messageKey = "log.message.automatedTradeRule.add",
            params = {
                    "#result.id",
                    "#result.portfolioId",
                    "#result.portfolioName",
                    "#result.platform",
                    "#result.symbol",
                    "#result.conditionType",
                    "#result.actionType",
                    "#result.quantityType",
                    "#result.quantity",
                    "#result.thresholdValue",
                    "#result.dateCreated"
            }
    )
    @Override
    public AutomatedTradeRuleResponse createAutomatedTradeRule(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            AutomatedTradeRule.ConditionType conditionType,
            AutomatedTradeRule.ActionType actionType,
            AutomatedTradeRule.QuantityType quantityType,
            BigDecimal quantity,
            BigDecimal thresholdValue) {

        // Database integrity checks
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        if (!portfolio.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedPortfolioAccessException(userId, portfolioId);
        }

        Optional<PlatformStock> platformStocks = platformStockRepository.findByStockNameAndPlatformName(symbol, platform);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        // Execution and persistence
        AutomatedTradeRule newRule = new AutomatedTradeRule();
        newRule.setUser(portfolio.getUser());
        newRule.setPortfolio(portfolio);
        newRule.setPlatformStock(platformStocks.get());
        newRule.setConditionType(conditionType);
        newRule.setActionType(actionType);
        newRule.setQuantityType(quantityType);
        newRule.setQuantity(quantity);
        newRule.setThresholdValue(thresholdValue);

        AutomatedTradeRule savedRule = automatedTradeRuleRepository.save(newRule);

        return new AutomatedTradeRuleResponse(
                savedRule.getAutomatedTradeRuleId(),
                savedRule.getPortfolio().getPortfolioId(),
                savedRule.getPortfolio().getPortfolioName(),
                platform,
                symbol,
                savedRule.getConditionType(),
                savedRule.getActionType(),
                savedRule.getQuantityType(),
                savedRule.getQuantity(),
                savedRule.getThresholdValue(),
                savedRule.getDateCreated()
        );
    }
}