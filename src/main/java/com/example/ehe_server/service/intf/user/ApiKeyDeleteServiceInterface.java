package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface ApiKeyDeleteServiceInterface {
    Map<String, Object> deleteApiKey(Long userId, Integer apiKeyId);
}
