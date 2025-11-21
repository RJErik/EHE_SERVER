package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioRetrievalResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.portfolio.PortfolioRetrievalServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioRetrievalService implements PortfolioRetrievalServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioRetrievalService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService,
            UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
        this.userContextService = userContextService;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.get",
            params = {"#result.size()"}
    )
    @Override
    public List<PortfolioRetrievalResponse> getPortfolios(Integer userId) {
        // Get portfolios for the user
        List<Portfolio> portfolios = portfolioRepository.findByUser(userContextService.getCurrentHumanUser());

        // Transform to response format
        List<Map<String, Object>> portfoliosList = new ArrayList<>();
        for (Portfolio portfolio : portfolios) {
            // Calculate portfolio value
            PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, portfolio.getPortfolioId());

            Object totalValue = valueResult.getTotalValue();

            Map<String, Object> portfolioMap = new HashMap<>();
            portfolioMap.put("id", portfolio.getPortfolioId());
            portfolioMap.put("name", portfolio.getPortfolioName());
            portfolioMap.put("platform", portfolio.getApiKey().getPlatformName());
            portfolioMap.put("creationDate", portfolio.getCreationDate().format(DATE_FORMATTER));
            portfolioMap.put("value", totalValue);

            portfoliosList.add(portfolioMap);
        }

        // Note: The call to portfolioValueService.calculatePortfolioValue returns a Map<String, Object>,
        // which is a legacy design. In a full refactoring, this service would also return a DTO.
        // We handle the Map for now to ensure compatibility.

        return portfolios.stream()
                .map(portfolio -> {
                    // Note: The call to portfolioValueService.calculatePortfolioValue returns a Map<String, Object>,
                    // which is a legacy design. In a full refactoring, this service would also return a DTO.
                    // We handle the Map for now to ensure compatibility.
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(
                            portfolio.getUser().getUserId(),
                            portfolio.getPortfolioId()
                    );
                    BigDecimal totalValue = valueResult.getTotalValue();

                    return new PortfolioRetrievalResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey().getPlatformName(),
                            portfolio.getCreationDate().format(DATE_FORMATTER),
                            totalValue
                    );
                })
                .collect(Collectors.toList());
    }
}