package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.user.ApiKeyDeleteServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ApiKeyDeleteService implements ApiKeyDeleteServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public ApiKeyDeleteService(
            ApiKeyRepository apiKeyRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.apiKeyRepository = apiKeyRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> deleteApiKey(Long userId, Integer apiKeyId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();

            // Check if API key exists and belongs to the user
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, user.getUserId());
            if (apiKeyOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "API key not found or does not belong to you");
                loggingService.logAction("API Key deletion failed: API key not found or unauthorized");
                return response;
            }

            // Delete the API key
            ApiKey apiKey = apiKeyOpt.get();
            String platformName = apiKey.getPlatformName(); // Capture for logging
            apiKeyRepository.delete(apiKey);

            response.put("success", true);
            response.put("message", "API key deleted successfully");

            loggingService.logAction("API Key deleted successfully for platform: " + platformName);

        } catch (Exception e) {
            loggingService.logError("Error deleting API key: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while deleting the API key. Please try again.");
        }

        return response;
    }
}
