package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.ApiKeyDto;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.user.ApiKeyListServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ApiKeyListService implements ApiKeyListServiceInterface {

    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public ApiKeyListService(
            UserRepository userRepository,
            ApiKeyRepository apiKeyRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    public Map<String, Object> listApiKeys(Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user exists and is active
            Optional<User> userOpt = userRepository.findById(userId.intValue());

            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                loggingService.logAction(null, userId.toString(), "API Key listing failed: User not found");
                return response;
            }

            User user = userOpt.get();

            // Check if account is active
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                response.put("success", false);
                response.put("message", "Your account is not active");
                loggingService.logAction(user.getUserId(), userId.toString(),
                        "API Key listing failed: Account not active, status=" + user.getAccountStatus());
                return response;
            }

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

            loggingService.logAction(user.getUserId(), userId.toString(),
                    "Retrieved " + apiKeyDtos.size() + " API keys successfully");

        } catch (Exception e) {
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error listing API keys: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred while retrieving API keys. Please try again.");
        }

        return response;
    }

    // Helper method to mask API key values
    private String maskApiKeyValue(String apiKeyValue) {
        if (apiKeyValue == null || apiKeyValue.length() <= 10) {
            // If key is too short, just show a few characters
            return apiKeyValue != null && apiKeyValue.length() > 0
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

        return firstPart + maskedMiddle.toString() + lastPart;
    }
}
