package ehe_server.service.apikey;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.ApiKey;
import ehe_server.exception.custom.ApiKeyNotFoundException;
import ehe_server.exception.custom.UnauthorizedApiKeyAccessException;
import ehe_server.repository.ApiKeyRepository;
import ehe_server.service.intf.apikey.ApiKeyRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyRemovalService implements ApiKeyRemovalServiceInterface {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyRemovalService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @LogMessage(
            messageKey = "log.message.user.apiKey.remove",
            params = {"#apiKeyId"}
    )
    @Override
    public void removeApiKey(Integer userId, Integer apiKeyId) {

        // ApiKey existence check
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        // Ownership verification
        if (!apiKey.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedApiKeyAccessException(userId, apiKeyId);
        }

        // Execute removal
        apiKeyRepository.delete(apiKey);
    }
}