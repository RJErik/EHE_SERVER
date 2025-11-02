package com.example.ehe_server.service.user;

import com.example.ehe_server.dto.ApiKeyRetrievalResponse;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.ApiKeyRetrievalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApiKeyRetrievalService implements ApiKeyRetrievalServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public ApiKeyRetrievalService(
            ApiKeyRepository apiKeyRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public List<ApiKeyRetrievalResponse> getApiKeys(Long userId) {
        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId.intValue())) {
            user = userRepository.findById(userId.intValue()).get();
        } else {
            return null;
        }


        // Retrieve API keys for the user and map them to ApiKeyRetrievalResponse DTOs
        List<ApiKeyRetrievalResponse> apiKeyDtos = apiKeyRepository.findByUser_UserId(user.getUserId())
                .stream()
                .map(apiKey -> {
                    ApiKeyRetrievalResponse dto = new ApiKeyRetrievalResponse();
                    dto.setApiKeyId(apiKey.getApiKeyId());
                    dto.setPlatformName(apiKey.getPlatformName());

                    // Mask the API key value
                    String apiKeyValue = apiKey.getApiKeyValue();
                    dto.setMaskedApiKeyValue(maskApiKeyValue(apiKeyValue));

                    // Mask the secret key value if it exists
                    if (apiKey.getSecretKey() != null && !apiKey.getSecretKey().isEmpty()) {
                        String secretKeyValue = apiKey.getSecretKey();
                        dto.setMaskedSecretKey(maskApiKeyValue(secretKeyValue));
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        loggingService.logAction("Retrieved " + apiKeyDtos.size() + " API keys successfully");

        // Return the DTO
        return apiKeyDtos;
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
