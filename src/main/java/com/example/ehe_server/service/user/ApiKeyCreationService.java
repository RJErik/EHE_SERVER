package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.ApiKeyCreationResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.PlatformNotFoundException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.ApiKeyCreationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyCreationService implements ApiKeyCreationServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public ApiKeyCreationService(
            ApiKeyRepository apiKeyRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.apiKey.add",
            params = {
                    "#platformName",
                    "#result.apiKeyId",
                    "#result.maskedApiKeyValue",
                    "#result.maskedSecretKey"
            }
    )
    @Override
    public ApiKeyCreationResponse createApiKey(Integer userId, String platformName, String apiKeyValue, String secretKey) {
        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            return null;
        }

        // Validate platform exists
        boolean platformExists = platformStockRepository.existsByPlatformName(platformName);
        if (!platformExists) {
            throw new PlatformNotFoundException(platformName);
        }

        // Create and save the new API key
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setPlatformName(platformName);
        apiKey.setApiKeyValue(apiKeyValue); // Not encrypting for now as specified
        apiKey.setSecretKey(secretKey); // Add the secret key

        apiKeyRepository.save(apiKey);

        loggingService.logAction("API Key added successfully for platform: " + platformName);

        // Create response with masked values
        ApiKeyCreationResponse response = new ApiKeyCreationResponse();
        response.setApiKeyId(apiKey.getApiKeyId());
        response.setPlatformName(apiKey.getPlatformName());
        response.setMaskedApiKeyValue(maskApiKeyValue(apiKeyValue));

        // Mask the secret key value if it exists
        if (secretKey != null && !secretKey.isEmpty()) {
            response.setMaskedSecretKey(maskApiKeyValue(secretKey));
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