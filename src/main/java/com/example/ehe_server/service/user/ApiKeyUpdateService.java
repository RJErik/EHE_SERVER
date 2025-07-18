package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.user.ApiKeyUpdateServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ApiKeyUpdateService implements ApiKeyUpdateServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public ApiKeyUpdateService(
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
    public Map<String, Object> updateApiKey(Long userId, Integer apiKeyId, String platformName, String apiKeyValue, String secretKey) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user exists and is active
            User user = userContextService.getCurrentHumanUser();

            // Check if API key exists and belongs to the user
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, user.getUserId());
            if (apiKeyOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "API key not found or does not belong to you");
                loggingService.logAction("API Key update failed: API key not found or unauthorized");
                return response;
            }

            // Validate platform exists
            boolean platformExists = platformStockRepository.existsByPlatformName(platformName);
            if (!platformExists) {
                response.put("success", false);
                response.put("message", "The specified platform is not supported");
                loggingService.logAction("API Key update failed: Platform not supported: " + platformName);
                return response;
            }

            // Update the API key
            ApiKey apiKey = apiKeyOpt.get();
            apiKey.setPlatformName(platformName);

            // Only update API key value if provided
            if (apiKeyValue != null && !apiKeyValue.isEmpty()) {
                apiKey.setApiKeyValueEncrypt(apiKeyValue);
            }

            // Only update secret key if provided
            if (secretKey != null && !secretKey.isEmpty()) {
                apiKey.setSecretKeyEncrypt(secretKey);
            }

            apiKeyRepository.save(apiKey);

            response.put("success", true);
            response.put("message", "API key updated successfully");

            loggingService.logAction("API Key updated successfully for platform: " + platformName);

        } catch (Exception e) {
            loggingService.logError("Error updating API key: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while updating the API key. Please try again.");
        }

        return response;
    }
}
