package ehe_server.service.intf.apikey;

import ehe_server.dto.ApiKeyResponse;

public interface ApiKeyCreationServiceInterface {
    ApiKeyResponse createApiKey(Integer userId, String platformName, String apiKeyValue, String secretKey);
}
