package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface ApiKeyAddServiceInterface {
    Map<String, Object> addApiKey(Long userId, String platformName, String apiKeyValue);
}
