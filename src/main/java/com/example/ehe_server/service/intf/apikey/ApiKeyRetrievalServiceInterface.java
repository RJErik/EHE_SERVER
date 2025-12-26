package com.example.ehe_server.service.intf.apikey;

import com.example.ehe_server.dto.ApiKeyResponse;

import java.util.List;

public interface ApiKeyRetrievalServiceInterface {
    List<ApiKeyResponse> getApiKeys(Integer userId);
}
