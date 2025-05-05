package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface ApiKeyListServiceInterface {
    Map<String, Object> listApiKeys(Long userId);
}
