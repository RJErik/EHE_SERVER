package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.Portfolio.PortfolioType;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioSearchServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioSearchService implements PortfolioSearchServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserContextService userContextService;

    public PortfolioSearchService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService, UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> searchPortfolios(PortfolioType type, String platform,
                                                BigDecimal minValue, BigDecimal maxValue) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get all user's portfolios
            List<Portfolio> allPortfolios = portfolioRepository.findByUser(userContextService.getCurrentHumanUser());

            // Filter portfolios based on search criteria
            List<Portfolio> filteredPortfolios = allPortfolios.stream()
                    .filter(p -> type == null || p.getPortfolioType() == type)
                    .filter(p -> platform == null || p.getApiKey().getPlatformName().equals(platform))
                    .collect(Collectors.toList());

            // Calculate values and apply min/max value filters
            List<Map<String, Object>> portfoliosList = new ArrayList<>();

            for (Portfolio portfolio : filteredPortfolios) {
                // Calculate portfolio value
                Map<String, Object> valueResult = portfolioValueService.calculatePortfolioValue(portfolio.getPortfolioId());
                BigDecimal value = new BigDecimal(valueResult.getOrDefault("totalValue", "0").toString());

                // Apply min/max value filters
                if ((minValue == null || value.compareTo(minValue) >= 0) &&
                        (maxValue == null || value.compareTo(maxValue) <= 0)) {

                    Map<String, Object> portfolioMap = new HashMap<>();
                    portfolioMap.put("id", portfolio.getPortfolioId());
                    portfolioMap.put("name", portfolio.getPortfolioName());
                    portfolioMap.put("platform", portfolio.getApiKey().getPlatformName());
                    portfolioMap.put("type", portfolio.getPortfolioType().toString());
                    portfolioMap.put("creationDate", portfolio.getCreationDate().format(DATE_FORMATTER));
                    portfolioMap.put("value", value);

                    portfoliosList.add(portfolioMap);
                }
            }

            // Prepare success response
            result.put("success", true);
            result.put("portfolios", portfoliosList);

            // Log success
            loggingService.logAction("Searched portfolios with criteria - type: " + type +
                            ", platform: " + platform +
                            ", minValue: " + minValue +
                            ", maxValue: " + maxValue +
                            ". Found " + portfoliosList.size() + " results.");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error searching portfolios: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching portfolios: " + e.getMessage());
        }

        return result;
    }
}
