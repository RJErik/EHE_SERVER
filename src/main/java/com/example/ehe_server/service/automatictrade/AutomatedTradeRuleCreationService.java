package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.dto.AutomatedTradeRuleCreationResponse;
import com.example.ehe_server.entity.*;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleCreationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AutomatedTradeRuleCreationService implements AutomatedTradeRuleCreationServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public AutomatedTradeRuleCreationService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            PortfolioRepository portfolioRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.portfolioRepository = portfolioRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public AutomatedTradeRuleCreationResponse createAutomatedTradeRule(
            Integer userId,
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionTypeStr,
            String actionTypeStr,
            String quantityTypeStr,
            BigDecimal quantity,
            BigDecimal thresholdValue) {

        // Validate portfolio
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();

        // Validate API key
        ApiKey apiKey = portfolio.getApiKey();
        if (apiKey == null) {
            throw new ApiKeyMissingException(portfolioId);
        }

        // Validate threshold value
        if (thresholdValue == null || thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidThresholdValueException(thresholdValue);
        }

        // Validate quantity
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuantityException(quantity);
        }

        // Parse condition type
        AutomatedTradeRule.ConditionType conditionType;
        try {
            conditionType = AutomatedTradeRule.ConditionType.valueOf(conditionTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidConditionTypeException(conditionTypeStr);
        }

        // Parse action type
        AutomatedTradeRule.ActionType actionType;
        try {
            actionType = AutomatedTradeRule.ActionType.valueOf(actionTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidActionTypeException(actionTypeStr);
        }

        // Parse quantity type
        AutomatedTradeRule.QuantityType quantityType;
        try {
            quantityType = AutomatedTradeRule.QuantityType.valueOf(quantityTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidQuantityTypeException(quantityTypeStr);
        }

        // Check if platform and symbol combination exists
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        PlatformStock platformStock = platformStocks.get(0);

        User user;

        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            throw new UserNotFoundException(userId);
        }

        // Create new automated trade rule
        AutomatedTradeRule newRule = new AutomatedTradeRule();
        newRule.setUser(user);
        newRule.setPortfolio(portfolio);
        newRule.setPlatformStock(platformStock);
        newRule.setConditionType(conditionType);
        newRule.setActionType(actionType);
        newRule.setQuantityType(quantityType);
        newRule.setQuantity(quantity);
        newRule.setThresholdValue(thresholdValue);
        newRule.setApiKey(apiKey);
        newRule.setDateCreated(LocalDateTime.now());
        newRule.setActive(true);

        AutomatedTradeRule savedRule = automatedTradeRuleRepository.save(newRule);

        // Log success
        loggingService.logAction("Added automated trade rule: " + platform + "/" + symbol + " " +
                conditionType + " " + thresholdValue + " -> " + actionType);

        return new AutomatedTradeRuleCreationResponse(
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
                savedRule.getDateCreated().format(DATE_FORMATTER),
                savedRule.isActive()
        );
    }
}