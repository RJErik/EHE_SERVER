package ehe_server.service.intf.apikey;

import ehe_server.dto.ApiKeyResponse;

import java.util.List;

public interface ApiKeyRetrievalServiceInterface {
    List<ApiKeyResponse> getApiKeys(Integer userId);
}
