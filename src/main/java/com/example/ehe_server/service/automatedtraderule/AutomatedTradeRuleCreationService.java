package com.example.ehe_server.service.automatedtraderule;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AutomatedTradeRuleResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeRuleCreationService implements AutomatedTradeRuleCreationServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AutomatedTradeRuleCreationService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            PortfolioRepository portfolioRepository,
            PlatformStockRepository platformStockRepository,
            UserRepository userRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.portfolioRepository = portfolioRepository;
        this.platformStockRepository = platformStockRepository;
        this.userRepository = userRepository;
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
                    "#result.dateCreated",
                    "#result.active"
            }
    )
    @Override
    public AutomatedTradeRuleResponse createAutomatedTradeRule(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionTypeStr,
            String actionTypeStr,
            String quantityTypeStr,
            BigDecimal quantity,
            BigDecimal thresholdValue) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (portfolioId == null) {
            throw new MissingPortfolioIdException();
        }

        if (platform == null || platform.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }

        if (conditionTypeStr == null || conditionTypeStr.trim().isEmpty()) {
            throw new MissingConditionTypeException();
        }

        if (actionTypeStr == null || actionTypeStr.trim().isEmpty()) {
            throw new MissingActionTypeException();
        }

        if (quantityTypeStr == null || quantityTypeStr.trim().isEmpty()) {
            throw new MissingQuantityTypeException();
        }

        if (quantity == null) {
            throw new MissingQuantityException();
        }

        if (thresholdValue == null) {
            throw new MissingThresholdValueException();
        }

        // Numeric validation
        if (thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidThresholdValueException(thresholdValue);
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        // Parsing logic
        AutomatedTradeRule.ConditionType conditionType;
        try {
            conditionType = AutomatedTradeRule.ConditionType.valueOf(conditionTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidConditionTypeException(conditionTypeStr);
        }

        AutomatedTradeRule.ActionType actionType;
        try {
            actionType = AutomatedTradeRule.ActionType.valueOf(actionTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidActionTypeException(actionTypeStr);
        }

        AutomatedTradeRule.QuantityType quantityType;
        try {
            quantityType = AutomatedTradeRule.QuantityType.valueOf(quantityTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidQuantityTypeException(quantityTypeStr);
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        if (!portfolio.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedPortfolioAccessException(userId, portfolioId);
        }

        Optional<PlatformStock> platformStocks = platformStockRepository.findByStockNameAndPlatformName(platform, symbol);
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
                savedRule.getConditionType().toString(),
                savedRule.getActionType().toString(),
                savedRule.getQuantityType().toString(),
                savedRule.getQuantity(),
                savedRule.getThresholdValue(),
                savedRule.getDateCreated().format(DATE_FORMATTER)
        );
    }
}