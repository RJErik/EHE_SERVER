package com.example.ehe_server.service.intf.apikey;

import com.example.ehe_server.dto.ApiKeyResponse;

public interface ApiKeyUpdateServiceInterface {
    ApiKeyResponse updateApiKey(Integer userId, Integer apiKeyId, String platformName, String apiKeyValue, String secretKey);
}
