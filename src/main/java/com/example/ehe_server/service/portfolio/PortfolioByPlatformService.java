package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioByPlatformService implements PortfolioByPlatformServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public PortfolioByPlatformService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }


    @Override
    public Map<String, Object> getPortfoliosByPlatform(String platform) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get all portfolios for the user
            List<Portfolio> allPortfolios = portfolioRepository.findByUser(userContextService.getCurrentHumanUser());

            // Filter portfolios by platform
            List<Portfolio> filteredPortfolios = allPortfolios.stream()
                    .filter(p -> p.getApiKey().getPlatformName().equalsIgnoreCase(platform))
                    .collect(Collectors.toList());

            // Transform to response format (only id and name as requested)
            List<Map<String, Object>> portfoliosList = new ArrayList<>();
            for (Portfolio portfolio : filteredPortfolios) {
                Map<String, Object> portfolioMap = new HashMap<>();
                portfolioMap.put("id", portfolio.getPortfolioId());
                portfolioMap.put("name", portfolio.getPortfolioName());
                portfoliosList.add(portfolioMap);
            }

            // Prepare success response
            result.put("success", true);
            result.put("portfolios", portfoliosList);

            // Log success
            loggingService.logAction("Retrieved " + portfoliosList.size() + " portfolios for platform: " + platform);

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving portfolios by platform: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving portfolios by platform: " + e.getMessage());
        }

        return result;
    }
}
