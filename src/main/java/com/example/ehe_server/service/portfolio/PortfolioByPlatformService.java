package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioByPlatformService implements PortfolioByPlatformServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public PortfolioByPlatformService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }


    @Override
    public Map<String, Object> getPortfoliosByPlatform(String platform) {
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
                loggingService.logAction(null, userIdStr, "Portfolio retrieval by platform failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Portfolio retrieval by platform failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get all portfolios for the user
            List<Portfolio> allPortfolios = portfolioRepository.findByUser(user);

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
            loggingService.logAction(userId, userIdStr,
                    "Retrieved " + portfoliosList.size() + " portfolios for platform: " + platform);

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving portfolios by platform: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving portfolios by platform: " + e.getMessage());
        }

        return result;
    }
}
