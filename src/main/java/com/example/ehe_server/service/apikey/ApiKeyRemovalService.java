package com.example.ehe_server.service.apikey;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.ApiKey;
import com.example.ehe_server.exception.custom.ApiKeyNotFoundException;
import com.example.ehe_server.exception.custom.MissingApiKeyIdException;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UnauthorizedApiKeyAccessException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.ApiKeyRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.apikey.ApiKeyRemovalServiceInterface;
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

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (apiKeyId == null) {
            throw new MissingApiKeyIdException();
        }

        // User validation
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // ApiKey existence check
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(apiKeyId);
        if (apiKeyOpt.isEmpty()) {
            throw new ApiKeyNotFoundException(apiKeyId);
        }

        ApiKey apiKey = apiKeyOpt.get();

        // Ownership verification
        if (!apiKey.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedApiKeyAccessException(userId, apiKeyId);
        }

        // Execute removal
        apiKeyRepository.delete(apiKey);
    }
}