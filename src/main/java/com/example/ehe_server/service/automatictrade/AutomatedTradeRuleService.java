package com.example.ehe_server.service.automatictrade;

import com.example.ehe_server.entity.*;
import com.example.ehe_server.repository.AutomatedTradeRuleRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.automatictrade.AutomatedTradeRuleServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AutomatedTradeRuleService implements AutomatedTradeRuleServiceInterface {

    private final AutomatedTradeRuleRepository automatedTradeRuleRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public AutomatedTradeRuleService(
            AutomatedTradeRuleRepository automatedTradeRuleRepository,
            PortfolioRepository portfolioRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.automatedTradeRuleRepository = automatedTradeRuleRepository;
        this.portfolioRepository = portfolioRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> getAutomatedTradeRules() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Get all active automated trade rules for the user
            List<AutomatedTradeRule> rules = automatedTradeRuleRepository.findByUser_UserIdAndIsActiveTrue(userId);

            // Transform to response format
            List<Map<String, Object>> rulesList = rules.stream()
                    .map(rule -> {
                        Map<String, Object> ruleMap = new HashMap<>();
                        ruleMap.put("id", rule.getAutomatedTradeRuleId());
                        ruleMap.put("portfolioId", rule.getPortfolio().getPortfolioId());
                        ruleMap.put("portfolioName", rule.getPortfolio().getPortfolioName());
                        ruleMap.put("platform", rule.getPlatformStock().getPlatformName());
                        ruleMap.put("symbol", rule.getPlatformStock().getStockSymbol());
                        ruleMap.put("conditionType", rule.getConditionType().toString());
                        ruleMap.put("actionType", rule.getActionType().toString());
                        ruleMap.put("quantityType", rule.getQuantityType().toString());
                        ruleMap.put("quantity", rule.getQuantity());
                        ruleMap.put("thresholdValue", rule.getThresholdValue());
                        ruleMap.put("dateCreated", rule.getDateCreated().format(DATE_FORMATTER));
                        ruleMap.put("isActive", rule.isActive());
                        return ruleMap;
                    })
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("automatedTradeRules", rulesList);

            // Log success
            loggingService.logAction("Automated trade rules retrieved successfully");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving automated trade rules: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving automated trade rules");
        }

        return result;
    }

    @Override
    public Map<String, Object> createAutomatedTradeRule(
            Integer portfolioId,
            String platform,
            String symbol,
            String conditionTypeStr,
            String actionTypeStr,
            String quantityTypeStr,
            BigDecimal quantity,
            BigDecimal thresholdValue) {

        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Validate portfolio
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction("Automated trade rule creation failed: Invalid portfolio ID: " + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();

            // Validate API key
            ApiKey apiKey = portfolio.getApiKey();
            if (apiKey == null) {
                result.put("success", false);
                result.put("message", "No API key associated with the portfolio");
                loggingService.logAction("Automated trade rule creation failed: No API key for portfolio ID: " + portfolioId);
                return result;
            }

            // Validate threshold value
            if (thresholdValue == null || thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
                result.put("success", false);
                result.put("message", "Threshold value must be greater than zero");
                loggingService.logAction("Automated trade rule creation failed: Invalid threshold value");
                return result;
            }

            // Validate quantity
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                result.put("success", false);
                result.put("message", "Quantity must be greater than zero");
                loggingService.logAction("Automated trade rule creation failed: Invalid quantity");
                return result;
            }

            // Parse condition type
            AutomatedTradeRule.ConditionType conditionType;
            try {
                conditionType = AutomatedTradeRule.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "Invalid condition type");
                loggingService.logAction("Automated trade rule creation failed: Invalid condition type: " + conditionTypeStr);
                return result;
            }

            // Parse action type
            AutomatedTradeRule.ActionType actionType;
            try {
                actionType = AutomatedTradeRule.ActionType.valueOf(actionTypeStr);
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "Invalid action type");
                loggingService.logAction("Automated trade rule creation failed: Invalid action type: " + actionTypeStr);
                return result;
            }

            // Parse quantity type
            AutomatedTradeRule.QuantityType quantityType;
            try {
                quantityType = AutomatedTradeRule.QuantityType.valueOf(quantityTypeStr);
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("message", "Invalid quantity type");
                loggingService.logAction("Automated trade rule creation failed: Invalid quantity type: " + quantityTypeStr);
                return result;
            }

            // Check if platform and symbol combination exists
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
            if (platformStocks.isEmpty()) {
                result.put("success", false);
                result.put("message", "Platform and symbol combination does not exist");
                loggingService.logAction("Automated trade rule creation failed: Platform/symbol combination not found: " + platform + "/" + symbol);
                return result;
            }

            PlatformStock platformStock = platformStocks.get(0);

            // Create new automated trade rule
            AutomatedTradeRule newRule = new AutomatedTradeRule();
            newRule.setUser(userContextService.getCurrentHumanUser());
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

            // Prepare success response
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("id", savedRule.getAutomatedTradeRuleId());
            ruleMap.put("portfolioId", savedRule.getPortfolio().getPortfolioId());
            ruleMap.put("portfolioName", savedRule.getPortfolio().getPortfolioName());
            ruleMap.put("platform", platform);
            ruleMap.put("symbol", symbol);
            ruleMap.put("conditionType", savedRule.getConditionType().toString());
            ruleMap.put("actionType", savedRule.getActionType().toString());
            ruleMap.put("quantityType", savedRule.getQuantityType().toString());
            ruleMap.put("quantity", savedRule.getQuantity());
            ruleMap.put("thresholdValue", savedRule.getThresholdValue());
            ruleMap.put("dateCreated", savedRule.getDateCreated().format(DATE_FORMATTER));
            ruleMap.put("isActive", savedRule.isActive());

            result.put("success", true);
            result.put("message", "Automated trade rule created successfully");
            result.put("automatedTradeRule", ruleMap);

            // Log success
            loggingService.logAction("Added automated trade rule: " + platform + "/" + symbol + " " +
                            conditionType + " " + thresholdValue + " -> " + actionType);

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error creating automated trade rule: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while creating automated trade rule");
        }

        return result;
    }

    @Override
    public Map<String, Object> removeAutomatedTradeRule(Integer automatedTradeRuleId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if the rule exists
            Optional<AutomatedTradeRule> ruleOptional = automatedTradeRuleRepository.findById(automatedTradeRuleId);
            if (ruleOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Automated trade rule not found");
                loggingService.logAction("Automated trade rule deactivation failed: Rule not found with ID: " + automatedTradeRuleId);
                return result;
            }

            AutomatedTradeRule rule = ruleOptional.get();

            // Verify the rule belongs to the user
            if (!rule.getUser().getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "Not authorized to remove this automated trade rule");
                loggingService.logAction("Automated trade rule deactivation failed: Unauthorized access to rule ID: " + automatedTradeRuleId);
                return result;
            }

            // Get rule details for logging
            String platform = rule.getPlatformStock().getPlatformName();
            String symbol = rule.getPlatformStock().getStockSymbol();
            AutomatedTradeRule.ConditionType conditionType = rule.getConditionType();
            AutomatedTradeRule.ActionType actionType = rule.getActionType();
            BigDecimal thresholdValue = rule.getThresholdValue();

            // Deactivate the rule (not delete)
            rule.setActive(false);
            automatedTradeRuleRepository.save(rule);

            // Prepare success response
            result.put("success", true);
            result.put("message", "Automated trade rule deactivated successfully");

            // Log success
            loggingService.logAction("Deactivated automated trade rule: " + platform + "/" + symbol + " " +
                            conditionType + " " + thresholdValue + " -> " + actionType +
                            " (ID: " + automatedTradeRuleId + ")");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error deactivating automated trade rule: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while deactivating automated trade rule");
        }

        return result;
    }
}