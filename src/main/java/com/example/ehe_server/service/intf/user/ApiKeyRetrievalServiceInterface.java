package com.example.ehe_server.service.intf.user;

import com.example.ehe_server.dto.ApiKeyRetrievalResponse;

import java.util.List;

public interface ApiKeyRetrievalServiceInterface {
    List<ApiKeyRetrievalResponse> getApiKeys(Long userId);
}
