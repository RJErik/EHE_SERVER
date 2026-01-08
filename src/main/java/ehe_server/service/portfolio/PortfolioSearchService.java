package ehe_server.service.portfolio;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.PortfolioResponse;
import ehe_server.dto.PortfolioValueResponse;
import ehe_server.entity.Portfolio;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.portfolio.PortfolioSearchServiceInterface;
import ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioSearchService implements PortfolioSearchServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueServiceInterface portfolioValueService;

    public PortfolioSearchService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.search",
            params = {
                    "#platform",
                    "#minValue",
                    "#maxValue",
                    "#result.size()"}
    )
    @Override
    public List<PortfolioResponse> searchPortfolios(Integer userId, String platform,
                                                    BigDecimal minValue, BigDecimal maxValue) {

        // Data retrieval
        List<Portfolio> portfolios = portfolioRepository.searchPortfolios(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null
        );

        // Response mapping and value filtering
        return portfolios.parallelStream()
                .map(portfolio -> {
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(
                            userId,
                            portfolio.getPortfolioId()
                    );
                    BigDecimal value = valueResult.getTotalValue();

                    return new PortfolioResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey() != null ? portfolio.getApiKey().getPlatform().getPlatformName() : null,
                            portfolio.getCreationDate(),
                            value
                    );
                })
                .filter(p -> (minValue == null || p.getValue().compareTo(minValue) >= 0) &&
                        (maxValue == null || p.getValue().compareTo(maxValue) <= 0))
                .collect(Collectors.toList());
    }
}