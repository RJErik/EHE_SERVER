package com.example.ehe_server.service.stock;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService implements StockServiceInterface {

    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public StockService(
            PlatformStockRepository platformStockRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.platformStockRepository = platformStockRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> getStocksByPlatform(String platformName) {
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
                loggingService.logAction(null, userIdStr, "Stock retrieval failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Stock retrieval failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Check if platform exists
            if (!platformStockRepository.existsByPlatformName(platformName)) {
                result.put("success", false);
                result.put("message", "Platform not found");
                loggingService.logAction(userId, userIdStr,
                        "Stock retrieval failed: Platform not found, platform=" + platformName);
                return result;
            }

            // Get all stocks for the platform
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);

            // Extract stock symbols
            List<String> stocks = platformStocks.stream()
                    .map(PlatformStock::getStockSymbol)
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("platform", platformName);
            result.put("stocks", stocks);

            // Log success
            loggingService.logAction(userId, userIdStr,
                    "Stocks retrieved successfully for platform: " + platformName);

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving stocks: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving stocks");
        }

        return result;
    }
}
