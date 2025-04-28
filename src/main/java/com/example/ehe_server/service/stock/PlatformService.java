package com.example.ehe_server.service.stock;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlatformService implements PlatformServiceInterface {

    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public PlatformService(
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
    public Map<String, Object> getAllPlatforms() {
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
                loggingService.logAction(null, userIdStr, "Platform retrieval failed: User not found");
                return result;
            }

            User user = userOptional.get();
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                result.put("success", false);
                result.put("message", "Account is not active");
                loggingService.logAction(userId, userIdStr,
                        "Platform retrieval failed: Account not active, status=" + user.getAccountStatus());
                return result;
            }

            // Get all platform stock records
            List<PlatformStock> platformStocks = platformStockRepository.findAll();

            // Extract unique platform names
            List<String> platforms = platformStocks.stream()
                    .map(PlatformStock::getPlatformName)
                    .distinct()
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("platforms", platforms);

            // Log success
            loggingService.logAction(userId, userIdStr, "Platforms retrieved successfully");

        } catch (Exception e) {
            // Log error
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error retrieving platforms: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving platforms");
        }

        return result;
    }
}
