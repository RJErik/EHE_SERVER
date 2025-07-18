package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.user.ApiKeyAddServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class ApiKeyAddService implements ApiKeyAddServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public ApiKeyAddService(
            ApiKeyRepository apiKeyRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> addApiKey(Long userId, String platformName, String apiKeyValue, String secretKey) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();

            // Validate platform exists
            boolean platformExists = platformStockRepository.existsByPlatformName(platformName);
            if (!platformExists) {
                response.put("success", false);
                response.put("message", "The specified platform is not supported");
                loggingService.logAction("API Key add failed: Platform not supported: " + platformName);
                return response;
            }

            // Create and save the new API key
            ApiKey apiKey = new ApiKey();
            apiKey.setUser(user);
            apiKey.setPlatformName(platformName);
            apiKey.setApiKeyValueEncrypt(apiKeyValue); // Not encrypting for now as specified
            apiKey.setSecretKeyEncrypt(secretKey); // Add the secret key

            apiKeyRepository.save(apiKey);

            response.put("success", true);
            response.put("message", "API key added successfully");
            response.put("apiKeyId", apiKey.getApiKeyId());

            loggingService.logAction("API Key added successfully for platform: " + platformName);

        } catch (Exception e) {
            loggingService.logError("Error adding API key: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while adding the API key. Please try again.");
        }

        return response;
    }
}
