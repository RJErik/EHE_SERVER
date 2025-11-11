package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.ApiKeyUpdateResponse;

import java.util.Map;

public interface ApiKeyUpdateServiceInterface {
    ApiKeyUpdateResponse updateApiKey(Integer userId, Integer apiKeyId, String platformName, String apiKeyValue, String secretKey);
}
