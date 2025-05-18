package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Holding;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.HoldingRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PortfolioService implements PortfolioServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final HoldingRepository holdingRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            HoldingRepository holdingRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.holdingRepository = holdingRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.portfolioValueService = portfolioValueService;
    }

    @Override
    public Map<String, Object> createPortfolio(String portfolioName, Integer apiKeyId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Portfolio creation failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio creation failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if API key exists and belongs to the user
            Optional<ApiKey> apiKeyOptional = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, userId);
            if (apiKeyOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "API key not found or doesn't belong to the user");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio creation failed: API key not found or doesn't belong to user, keyId=" + apiKeyId);
                return result;
            }

            ApiKey apiKey = apiKeyOptional.get();

            // Create a new portfolio (Real type only as specified)
            Portfolio portfolio = new Portfolio();
            portfolio.setUser(user);
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
            loggingService.logAction(userId, userIdStr,
                    "Created portfolio: " + portfolioName + " with API key from platform " + apiKey.getPlatformName());

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error creating portfolio: " + e.getMessage(), e);

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

            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);
            System.out.println("User ID from audit context: " + userId);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                System.out.println("User not found for ID: " + userId);
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Portfolio retrieval failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                System.out.println("User account not active. Status: " + user.getAccountStatus());
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio retrieval failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get portfolios for the user
            List<Portfolio> portfolios = portfolioRepository.findByUser(user);
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
            loggingService.logAction(userId, userIdStr,
                    "Retrieved " + portfoliosList.size() + " portfolios");

        } catch (Exception e) {
            System.out.println("Error retrieving portfolios: " + e.getMessage());
            e.printStackTrace();

            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving portfolios: " + e.getMessage(), e);

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
            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Portfolio deletion failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio deletion failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if portfolio exists and belongs to the user
            Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
            if (portfolioOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "Portfolio not found or doesn't belong to the user");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio deletion failed: Portfolio not found or doesn't belong to user, portfolioId=" + portfolioId);
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
            loggingService.logAction(userId, userIdStr,
                    "Deleted portfolio: " + portfolio.getPortfolioName() + " and " + holdings.size() + " holdings");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error deleting portfolio: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while deleting portfolio: " + e.getMessage());
        }

        return result;
    }
}
