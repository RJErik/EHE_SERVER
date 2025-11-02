package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.PortfolioCreationResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.ApiKeyNotFoundException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioCreationServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class PortfolioCreationService implements PortfolioCreationServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioCreationService(
            PortfolioRepository portfolioRepository,
            ApiKeyRepository apiKeyRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService,
            UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.userContextService = userContextService;
    }

    @Override
    public PortfolioCreationResponse createPortfolio(String portfolioName, Integer apiKeyId) {
        // Get current user ID from user context
        Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

        // Check if API key exists and belongs to the user
        Optional<ApiKey> apiKeyOptional = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, userId);
        if (apiKeyOptional.isEmpty()) {
            throw new ApiKeyNotFoundException(apiKeyId, userId);
        }

        ApiKey apiKey = apiKeyOptional.get();

        // Create a new portfolio (Real type only as specified)
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(userContextService.getCurrentHumanUser());
        portfolio.setApiKey(apiKey);
        portfolio.setPortfolioName(portfolioName);
        portfolio.setCreationDate(LocalDateTime.now());

        // Save the portfolio
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        // Update holdings for the new portfolio
        portfolioValueService.updateHoldings(userId, savedPortfolio.getPortfolioId());

        // Calculate portfolio value
        PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, savedPortfolio.getPortfolioId());
        BigDecimal totalValue = valueResult.getTotalValue();

        // Prepare success response


        // Log success
        loggingService.logAction("Created portfolio: " + portfolioName + " with API key from platform " + apiKey.getPlatformName());

        // Prepare and return success DTO
        return new PortfolioCreationResponse(
                savedPortfolio.getPortfolioId(),
                savedPortfolio.getPortfolioName(),
                apiKey.getPlatformName(),
                savedPortfolio.getCreationDate().format(DATE_FORMATTER),
                totalValue
        );
    }
}