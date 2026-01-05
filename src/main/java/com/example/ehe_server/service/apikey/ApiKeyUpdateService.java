package com.example.ehe_server.service.apikey;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.ApiKeyResponse;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.Platform;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.PlatformRepository;
import com.example.ehe_server.service.intf.apikey.ApiKeyUpdateServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyUpdateService implements ApiKeyUpdateServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final PlatformRepository platformRepository;

    public ApiKeyUpdateService(
            ApiKeyRepository apiKeyRepository,
            PlatformRepository platformRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.platformRepository = platformRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.apiKey.update",
            params = {
                    "#result.apiKeyId",
                    "#result.platformName",
                    "#result.maskedApiKeyValue",
                    "#result.maskedSecretKey"}
    )
    @Override
    public ApiKeyResponse updateApiKey(Integer userId, Integer apiKeyId, String platformName, String apiKeyValue, String secretKey) {

        // ApiKey lookup and authorization
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (!apiKey.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedApiKeyAccessException(userId, apiKeyId);
        }

        // Execute updates - only if platformName is provided
        if (platformName != null && !platformName.isEmpty() && !apiKey.getPlatform().getPlatformName().equals(platformName)) {
            Platform platform = platformRepository.findByPlatformName(platformName)
                    .orElseThrow(() -> new PlatformNotFoundException(platformName));
            if (!apiKey.getPlatform().getPlatformId().equals(platform.getPlatformId())) {
                apiKey.setPlatform(platform);
            }
        }

        if (apiKeyValue != null && !apiKeyValue.isEmpty() && !apiKeyValue.equals(apiKey.getApiKeyValue())) {
            apiKey.setApiKeyValue(apiKeyValue);
        }

        if (secretKey != null && !secretKey.isEmpty() && !secretKey.equals(apiKey.getSecretKey())) {
            apiKey.setSecretKey(secretKey);
        }


        apiKeyRepository.save(apiKey);

        // Response mapping
        ApiKeyResponse response = new ApiKeyResponse();
        response.setApiKeyId(apiKey.getApiKeyId());
        response.setPlatformName(apiKey.getPlatform().getPlatformName());

        String apiKeyValueFromDb = apiKey.getApiKeyValue();
        response.setMaskedApiKeyValue(maskApiKeyValue(apiKeyValueFromDb));

        String secretKeyValue = apiKey.getSecretKey();
        response.setMaskedSecretKey(maskApiKeyValue(secretKeyValue));

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