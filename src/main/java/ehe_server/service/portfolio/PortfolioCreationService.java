package ehe_server.service.portfolio;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.PortfolioResponse;
import ehe_server.dto.PortfolioValueResponse;
import ehe_server.entity.ApiKey;
import ehe_server.entity.Portfolio;
import ehe_server.exception.custom.ApiKeyNotFoundException;
import ehe_server.exception.custom.DuplicatePortfolioNameException;
import ehe_server.exception.custom.UnauthorizedApiKeyAccessException;
import ehe_server.repository.ApiKeyRepository;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.portfolio.PortfolioCreationServiceInterface;
import ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PortfolioCreationService implements PortfolioCreationServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PortfolioValueServiceInterface portfolioValueService;

    public PortfolioCreationService(
            PortfolioRepository portfolioRepository,
            ApiKeyRepository apiKeyRepository,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.portfolioValueService = portfolioValueService;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.add",
            params = {
                    "#apiKeyId",
                    "#result.id",
                    "#result.name",
                    "#result.platform",
                    "#result.creationDate",
                    "#result.value"
            }
    )
    @Override
    public PortfolioResponse createPortfolio(Integer userId, String portfolioName, Integer apiKeyId) {

        // Database integrity checks
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
        portfolio.setReservedCash(BigDecimal.ZERO);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        // External service integration
        PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, savedPortfolio.getPortfolioId());
        BigDecimal totalValue = valueResult.getTotalValue();

        return new PortfolioResponse(
                savedPortfolio.getPortfolioId(),
                savedPortfolio.getPortfolioName(),
                apiKey.getPlatform().getPlatformName(),
                savedPortfolio.getCreationDate(),
                totalValue
        );
    }
}