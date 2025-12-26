package com.example.ehe_server.service.apikey;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.ApiKeyResponse;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.apikey.ApiKeyRetrievalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ApiKeyRetrievalService implements ApiKeyRetrievalServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyRetrievalService(
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.apiKey.get",
            params = {"#result.size()"}
    )
    @Override
    public List<ApiKeyResponse> getApiKeys(Integer userId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval and response mapping
        return apiKeyRepository.findByUser_UserIdOrderByDateAddedDesc(userId)
                .stream()
                .map(apiKey -> {
                    ApiKeyResponse dto = new ApiKeyResponse();
                    dto.setApiKeyId(apiKey.getApiKeyId());
                    dto.setPlatformName(apiKey.getPlatform().getPlatformName());

                    // Mask the API and Secret keys
                    dto.setMaskedApiKeyValue(maskApiKeyValue(apiKey.getApiKeyValue()));
                    dto.setMaskedSecretKey(maskApiKeyValue(apiKey.getSecretKey()));

                    return dto;
                })
                .collect(Collectors.toList());
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