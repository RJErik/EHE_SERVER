package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.ApiKeyUpdateResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.ApiKeyNotFoundException;
import com.example.ehe_server.exception.custom.PlatformNotSupportedException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.ApiKeyUpdateServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ApiKeyUpdateService implements ApiKeyUpdateServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public ApiKeyUpdateService(
            ApiKeyRepository apiKeyRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiKeyUpdateResponse updateApiKey(Long userId, Integer apiKeyId, String platformName, String apiKeyValue, String secretKey) {
        // Check if user exists and is active
        User user;
        if (userRepository.existsById(userId.intValue())) {
            user = userRepository.findById(userId.intValue()).get();
        } else {
            return null;
        }

        // Check if API key exists and belongs to the user
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, user.getUserId());
        if (apiKeyOpt.isEmpty()) {
            throw new ApiKeyNotFoundException(apiKeyId, user.getUserId());
        }

        // Validate platform exists
        boolean platformExists = platformStockRepository.existsByPlatformName(platformName);
        if (!platformExists) {
            throw new PlatformNotSupportedException(platformName);
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

        loggingService.logAction("API Key updated successfully for platform: " + platformName);

        // Create response with masked values
        ApiKeyUpdateResponse response = new ApiKeyUpdateResponse();
        response.setApiKeyId(apiKey.getApiKeyId());
        response.setPlatformName(apiKey.getPlatformName());

        // Mask the API key value
        String apiKeyValueFromDb = apiKey.getApiKeyValueEncrypt();
        response.setMaskedApiKeyValue(maskApiKeyValue(apiKeyValueFromDb));

        // Mask the secret key value if it exists
        if (apiKey.getSecretKeyEncrypt() != null && !apiKey.getSecretKeyEncrypt().isEmpty()) {
            String secretKeyValue = apiKey.getSecretKeyEncrypt();
            response.setMaskedSecretKey(maskApiKeyValue(secretKeyValue));
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