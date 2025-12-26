package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioCreationServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class PortfolioCreationService implements PortfolioCreationServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PortfolioValueServiceInterface portfolioValueService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PORTFOLIO_NAME_MAX_LENGTH = 100;

    public PortfolioCreationService(
            PortfolioRepository portfolioRepository,
            ApiKeyRepository apiKeyRepository,
            PortfolioValueServiceInterface portfolioValueService,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.portfolioValueService = portfolioValueService;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.add",
            params = {
                    "#apiKeyId",
                    "#result.portfolioId",
                    "#result.portfolioName",
                    "#result.platformName",
                    "#result.creationDate",
                    "#result.totalValue"
            }
    )
    @Override
    public PortfolioResponse createPortfolio(Integer userId, String portfolioName, Integer apiKeyId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (apiKeyId == null) {
            throw new MissingApiKeyIdException();
        }

        if (portfolioName == null || portfolioName.trim().isEmpty()) {
            throw new MissingPortfolioNameException();
        }

        if (portfolioName.length() > PORTFOLIO_NAME_MAX_LENGTH) {
            throw new InvalidPortfolioName(portfolioName);
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        // Authorization verification
        if (!apiKey.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedApiKeyAccessException(userId, apiKeyId);
        }

        // Duplicate name validation
        String cleanName = portfolioName.trim();
        if (portfolioRepository.existsByUser_UserIdAndPortfolioNameIgnoreCase(userId, cleanName)) {
            throw new DuplicatePortfolioNameException(cleanName);
        }

        // Execution and persistence
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(apiKey.getUser());
        portfolio.setApiKey(apiKey);
        portfolio.setPortfolioName(cleanName);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        // External service integration
        portfolioValueService.updateHoldings(userId, portfolio);
        PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, savedPortfolio.getPortfolioId());
        BigDecimal totalValue = valueResult.getTotalValue();

        return new PortfolioResponse(
                savedPortfolio.getPortfolioId(),
                savedPortfolio.getPortfolioName(),
                apiKey.getPlatform().getPlatformName(),
                savedPortfolio.getCreationDate().format(DATE_FORMATTER),
                totalValue
        );
    }
}