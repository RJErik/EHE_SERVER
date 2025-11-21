package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.ApiKeyNotFoundException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.user.ApiKeyRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ApiKeyRemovalService implements ApiKeyRemovalServiceInterface {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyRemovalService(
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.apiKey.remove",
            params = {"#apiKeyId"}
    )
    @Override
    public void removeApiKey(Integer userId, Integer apiKeyId) {
        // Get current user ID from user context
        User user;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
        } else {
            return;
        }

        // Check if API key exists and belongs to the user
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyIdAndUser_UserId(apiKeyId, user.getUserId());
        if (apiKeyOpt.isEmpty()) {
            throw new ApiKeyNotFoundException(apiKeyId, user.getUserId());
        }

        // Delete the API key
        ApiKey apiKey = apiKeyOpt.get();
        apiKeyRepository.delete(apiKey);
    }
}
