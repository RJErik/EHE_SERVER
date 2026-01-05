package com.example.ehe_server.service.apikey;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.ApiKeyResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Platform;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.apikey.ApiKeyCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyCreationService implements ApiKeyCreationServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformRepository platformRepository;
    private final UserRepository userRepository;

    public ApiKeyCreationService(
            ApiKeyRepository apiKeyRepository,
            PlatformRepository platformRepository,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformRepository = platformRepository;
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
    public ApiKeyResponse createApiKey(Integer userId, String platformName, String apiKeyValue, String secretKey) {

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Platform platform = platformRepository.findByPlatformName(platformName)
                .orElseThrow(() -> new PlatformNotFoundException(platformName));

        // Execution and persistence
        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setPlatform(platform);
        apiKey.setApiKeyValue(apiKeyValue);
        apiKey.setSecretKey(secretKey);

        apiKeyRepository.save(apiKey);

        // Response construction
        ApiKeyResponse response = new ApiKeyResponse();
        response.setApiKeyId(apiKey.getApiKeyId());
        response.setPlatformName(apiKey.getPlatform().getPlatformName());
        response.setMaskedApiKeyValue(maskApiKeyValue(apiKeyValue));
        response.setMaskedSecretKey(maskApiKeyValue(secretKey));

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

        return firstPart + "*".repeat(maskedLength) + lastPart;
    }
}