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

        // Return success DTO
        return new ApiKeyUpdateResponse(apiKey.getApiKeyId(), apiKey.getPlatformName());
    }
}
