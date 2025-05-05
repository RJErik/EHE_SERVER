package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface ApiKeyUpdateServiceInterface {
    Map<String, Object> updateApiKey(Long userId, Integer apiKeyId, String platformName, String apiKeyValue);
}
