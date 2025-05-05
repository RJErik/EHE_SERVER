package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.user.ApiKeyAddServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ApiKeyAddService implements ApiKeyAddServiceInterface {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public ApiKeyAddService(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> addApiKey(Long userId, String platformName, String apiKeyValue) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user exists and is active
            Optional<User> userOpt = userRepository.findById(userId.intValue());

            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                loggingService.logAction(null, userId.toString(), "API Key add failed: User not found");
                return response;
            }

            User user = userOpt.get();

            // Check if account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                response.put("success", false);
                response.put("message", "Your account is not active");
                loggingService.logAction(user.getUserId(), userId.toString(),
                        "API Key add failed: Account not active, status=" + user.getAccountStatus());
                return response;
            }

            // Validate platform exists
            boolean platformExists = platformStockRepository.existsByPlatformName(platformName);
            if (!platformExists) {
                response.put("success", false);
                response.put("message", "The specified platform is not supported");
                loggingService.logAction(user.getUserId(), userId.toString(),
                        "API Key add failed: Platform not supported: " + platformName);
                return response;
            }

            // Create and save the new API key
            ApiKey apiKey = new ApiKey();
            apiKey.setUser(user);
            apiKey.setPlatformName(platformName);
            apiKey.setApiKeyValueEncrypt(apiKeyValue); // Not encrypting for now as specified

            apiKeyRepository.save(apiKey);

            response.put("success", true);
            response.put("message", "API key added successfully");
            response.put("apiKeyId", apiKey.getApiKeyId());

            loggingService.logAction(user.getUserId(), userId.toString(),
                    "API Key added successfully for platform: " + platformName);

        } catch (Exception e) {
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error adding API key: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while adding the API key. Please try again.");
        }

        return response;
    }
}
