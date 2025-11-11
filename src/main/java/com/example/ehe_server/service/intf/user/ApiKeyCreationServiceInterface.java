package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.ApiKeyCreationResponse;

public interface ApiKeyCreationServiceInterface {
    ApiKeyCreationResponse createApiKey(Integer userId, String platformName, String apiKeyValue, String secretKey);
}
