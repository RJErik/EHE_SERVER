package com.example.ehe_server.service.intf.user;

public interface ApiKeyRemovalServiceInterface {
    void removeApiKey(Long userId, Integer apiKeyId);
}
