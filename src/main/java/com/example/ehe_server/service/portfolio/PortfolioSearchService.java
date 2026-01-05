package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioSearchServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
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