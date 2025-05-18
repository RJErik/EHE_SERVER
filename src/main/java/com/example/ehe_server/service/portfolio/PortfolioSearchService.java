package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.Portfolio.PortfolioType;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioSearchServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioSearchService implements PortfolioSearchServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PortfolioSearchService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService,
            PortfolioValueServiceInterface portfolioValueService) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
        this.portfolioValueService = portfolioValueService;
    }

    @Override
    public Map<String, Object> searchPortfolios(PortfolioType type, String platform,
                                                BigDecimal minValue, BigDecimal maxValue) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get current user ID from audit context
            String userIdStr = auditContextService.getCurrentUser();
            Integer userId = Integer.parseInt(userIdStr);

            // Check if user exists and is active
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                result.put("success", false);
                result.put("message", "User not found");
                loggingService.logAction(null, userIdStr, "Portfolio search failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio search failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get all user's portfolios
            List<Portfolio> allPortfolios = portfolioRepository.findByUser(user);

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
            loggingService.logAction(userId, userIdStr,
                    "Searched portfolios with criteria - type: " + type +
                            ", platform: " + platform +
                            ", minValue: " + minValue +
                            ", maxValue: " + maxValue +
                            ". Found " + portfoliosList.size() + " results.");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error searching portfolios: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while searching portfolios: " + e.getMessage());
        }

        return result;
    }
}
