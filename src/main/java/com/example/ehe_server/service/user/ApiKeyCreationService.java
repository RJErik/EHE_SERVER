package com.example.ehe_server.service.user;

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

    @Override
    @Transactional
    public ApiKeyCreationResponse createApiKey(Long userId, String platformName, String apiKeyValue, String secretKey) {
        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId.intValue())) {
            user = userRepository.findById(userId.intValue()).get();
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
        apiKey.setApiKeyValueEncrypt(apiKeyValue); // Not encrypting for now as specified
        apiKey.setSecretKeyEncrypt(secretKey); // Add the secret key

        apiKeyRepository.save(apiKey);

        loggingService.logAction("API Key added successfully for platform: " + platformName);

        // Return a success DTO
        return new ApiKeyCreationResponse("API key added successfully", apiKey.getApiKeyId().longValue());
    }
}
