package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.ApiKeyDto;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.user.ApiKeyListServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ApiKeyListService implements ApiKeyListServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public ApiKeyListService(
            ApiKeyRepository apiKeyRepository,
            LoggingServiceInterface loggingService, UserContextService userContextService) {
        this.apiKeyRepository = apiKeyRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    public Map<String, Object> listApiKeys(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current user ID from user context
            User user = userContextService.getCurrentHumanUser();

            // Retrieve API keys for the user
            List<ApiKey> userApiKeys = apiKeyRepository.findByUser_UserId(user.getUserId());
            List<ApiKeyDto> apiKeyDtos = new ArrayList<>();

            for (ApiKey apiKey : userApiKeys) {
                ApiKeyDto dto = new ApiKeyDto();
                dto.setApiKeyId(apiKey.getApiKeyId());
                dto.setPlatformName(apiKey.getPlatformName());

                // Mask the API key value - show first 5 and last 5 characters, mask the rest
                String apiKeyValue = apiKey.getApiKeyValueEncrypt();
                String maskedValue = maskApiKeyValue(apiKeyValue);
                dto.setMaskedApiKeyValue(maskedValue);

                // Mask the secret key value if it exists - same masking approach
                if (apiKey.getSecretKeyEncrypt() != null && !apiKey.getSecretKeyEncrypt().isEmpty()) {
                    String secretKeyValue = apiKey.getSecretKeyEncrypt();
                    String maskedSecretKey = maskApiKeyValue(secretKeyValue);
                    dto.setMaskedSecretKey(maskedSecretKey);
                }

                apiKeyDtos.add(dto);
            }

            response.put("success", true);
            response.put("apiKeys", apiKeyDtos);

            loggingService.logAction("Retrieved " + apiKeyDtos.size() + " API keys successfully");

        } catch (Exception e) {
            loggingService.logError("Error listing API keys: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while retrieving API keys. Please try again.");
        }

        return response;
    }

    // Helper method to mask API key values
    private String maskApiKeyValue(String apiKeyValue) {
        if (apiKeyValue == null || apiKeyValue.length() <= 10) {
            // If key is too short, just show a few characters
            return apiKeyValue != null && !apiKeyValue.isEmpty()
                    ? apiKeyValue.substring(0, Math.min(3, apiKeyValue.length())) + "****"
                    : "";
        }

        // Show first 5 and last 5 characters, mask the middle
        int visibleCharCount = 5;
        String firstPart = apiKeyValue.substring(0, visibleCharCount);
        String lastPart = apiKeyValue.substring(apiKeyValue.length() - visibleCharCount);
        int maskedLength = apiKeyValue.length() - (2 * visibleCharCount);

        StringBuilder maskedMiddle = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            maskedMiddle.append("*");
        }

        return firstPart + maskedMiddle + lastPart;
    }
}
