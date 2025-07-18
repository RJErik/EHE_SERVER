package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Holding;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.HoldingRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class PortfolioService implements PortfolioServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final HoldingRepository holdingRepository;
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            ApiKeyRepository apiKeyRepository,
            HoldingRepository holdingRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService, UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.holdingRepository = holdingRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> createPortfolio(String portfolioName, Integer apiKeyId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if API key exists and belongs to the user
            Optional<ApiKey> apiKeyOptional = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, userId);
            if (apiKeyOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "API key not found or doesn't belong to the user");
                loggingService.logAction("Portfolio creation failed: API key not found or doesn't belong to user, keyId=" + apiKeyId);
                return result;
            }

            ApiKey apiKey = apiKeyOptional.get();

            // Create a new portfolio (Real type only as specified)
            Portfolio portfolio = new Portfolio();
            portfolio.setUser(userContextService.getCurrentHumanUser());
            portfolio.setApiKey(apiKey);
            portfolio.setPortfolioName(portfolioName);
            portfolio.setPortfolioType(Portfolio.PortfolioType.Real);
            portfolio.setCreationDate(LocalDateTime.now());

            // Save the portfolio
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);

            // Update holdings for the new portfolio
            portfolioValueService.updateHoldings(savedPortfolio.getPortfolioId());

            // Calculate portfolio value
            Map<String, Object> valueResult = portfolioValueService.calculatePortfolioValue(savedPortfolio.getPortfolioId());

            // Prepare success response
            Map<String, Object> portfolioMap = new HashMap<>();
            portfolioMap.put("id", savedPortfolio.getPortfolioId());
            portfolioMap.put("name", savedPortfolio.getPortfolioName());
            portfolioMap.put("platform", apiKey.getPlatformName());
            portfolioMap.put("type", savedPortfolio.getPortfolioType().toString());
            portfolioMap.put("creationDate", savedPortfolio.getCreationDate().format(DATE_FORMATTER));
            portfolioMap.put("value", valueResult.getOrDefault("totalValue", 0));

            result.put("success", true);
            result.put("message", "Portfolio created successfully");
            result.put("portfolio", portfolioMap);

            // Log success
            loggingService.logAction("Created portfolio: " + portfolioName + " with API key from platform " + apiKey.getPlatformName());

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error creating portfolio: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while creating portfolio: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getPortfolios() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Starting getPortfolios method");

            // Get current user ID from user context
            int userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());
            System.out.println("User ID from audit context: " + userId);

            // Get portfolios for the user
            List<Portfolio> portfolios = portfolioRepository.findByUser(userContextService.getCurrentHumanUser());
            System.out.println("Found " + portfolios.size() + " portfolios for user");

            // Transform to response format
            List<Map<String, Object>> portfoliosList = new ArrayList<>();
            for (Portfolio portfolio : portfolios) {
                System.out.println("Processing portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

                // Calculate portfolio value
                System.out.println("Calling calculatePortfolioValue for portfolio ID: " + portfolio.getPortfolioId());
                Map<String, Object> valueResult = portfolioValueService.calculatePortfolioValue(portfolio.getPortfolioId());

                Object totalValue = valueResult.getOrDefault("totalValue", 0);
                System.out.println("Received total value: " + totalValue + " for portfolio: " + portfolio.getPortfolioName());

                Map<String, Object> portfolioMap = new HashMap<>();
                portfolioMap.put("id", portfolio.getPortfolioId());
                portfolioMap.put("name", portfolio.getPortfolioName());
                portfolioMap.put("platform", portfolio.getApiKey().getPlatformName());
                portfolioMap.put("type", portfolio.getPortfolioType().toString());
                portfolioMap.put("creationDate", portfolio.getCreationDate().format(DATE_FORMATTER));
                portfolioMap.put("value", totalValue);

                portfoliosList.add(portfolioMap);
                System.out.println("Added portfolio to response list: " + portfolio.getPortfolioName() + " with value: " + totalValue);
            }

            // Prepare success response
            result.put("success", true);
            result.put("portfolios", portfoliosList);

            // Log success
            loggingService.logAction("Retrieved " + portfoliosList.size() + " portfolios");

        } catch (Exception e) {
            System.out.println("Error retrieving portfolios: " + e.getMessage());
            e.printStackTrace();

            // Log error
            loggingService.logError("Error retrieving portfolios: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving portfolios: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> deletePortfolio(Integer portfolioId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from user context
            Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction("Portfolio deletion failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
                return result;
            }

            Portfolio portfolio = portfolioOptional.get();

            // First delete all holdings associated with this portfolio
            List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
            holdingRepository.deleteAll(holdings);

            // Now delete the portfolio
            portfolioRepository.delete(portfolio);

            // Prepare success response
            result.put("success", true);
            result.put("message", "Portfolio and its holdings deleted successfully");

            // Log success
            loggingService.logAction("Deleted portfolio: " + portfolio.getPortfolioName() + " and " + holdings.size() + " holdings");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error deleting portfolio: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while deleting portfolio: " + e.getMessage());
        }

        return result;
    }
}
