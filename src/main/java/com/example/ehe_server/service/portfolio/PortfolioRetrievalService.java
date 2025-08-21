package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.PortfolioRetrievalResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
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
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioRetrievalService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService,
            UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.userContextService = userContextService;
    }

    @Override
    public List<PortfolioRetrievalResponse> getPortfolios(Integer userId) {

        System.out.println("Starting getPortfolios method");

        // Get portfolios for the user
        List<Portfolio> portfolios = portfolioRepository.findByUser(userContextService.getCurrentHumanUser());
        System.out.println("Found " + portfolios.size() + " portfolios for user");

        // Transform to response format
        List<Map<String, Object>> portfoliosList = new ArrayList<>();
        for (Portfolio portfolio : portfolios) {
            System.out.println("Processing portfolio: " + portfolio.getPortfolioName() + " (ID: " + portfolio.getPortfolioId() + ")");

            // Calculate portfolio value
            System.out.println("Calling calculatePortfolioValue for portfolio ID: " + portfolio.getPortfolioId());
            PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, portfolio.getPortfolioId());

            Object totalValue = valueResult.getTotalValue();
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

        List<PortfolioRetrievalResponse> portfolioDtos = portfolios.stream()
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
                            portfolio.getPortfolioType().toString(),
                            portfolio.getCreationDate().format(DATE_FORMATTER),
                            totalValue
                    );
                })
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Retrieved " + portfoliosList.size() + " portfolios");

        return portfolioDtos;
    }
}