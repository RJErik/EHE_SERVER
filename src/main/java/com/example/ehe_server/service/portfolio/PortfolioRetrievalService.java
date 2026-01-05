package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioRetrievalServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioRetrievalService implements PortfolioRetrievalServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueServiceInterface portfolioValueService;

    public PortfolioRetrievalService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.get",
            params = {"#result.size()"}
    )
    @Override
    public List<PortfolioResponse> getPortfolios(Integer userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUser_UserIdOrderByCreationDateDesc(userId);

        return portfolios.parallelStream()
                .map(portfolio -> {
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(
                            userId,
                            portfolio.getPortfolioId()
                    );

                    BigDecimal totalValue = valueResult.getTotalValue();

                    return new PortfolioResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey().getPlatform().getPlatformName(),
                            portfolio.getCreationDate(),
                            totalValue
                    );
                })
                .collect(Collectors.toList());
    }
}