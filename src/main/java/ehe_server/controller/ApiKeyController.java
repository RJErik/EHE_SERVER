package ehe_server.controller;

import ehe_server.annotation.validation.NotNullField;
import ehe_server.dto.ApiKeyCreationRequest;
import ehe_server.dto.ApiKeyResponse;
import ehe_server.dto.ApiKeyUpdateRequest;
import ehe_server.exception.custom.MissingApiKeyIdException;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.apikey.ApiKeyCreationServiceInterface;
import ehe_server.service.intf.apikey.ApiKeyRemovalServiceInterface;
import ehe_server.service.intf.apikey.ApiKeyRetrievalServiceInterface;
import ehe_server.service.intf.apikey.ApiKeyUpdateServiceInterface;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/api-keys")
@Validated
public class ApiKeyController {

    private final UserContextServiceInterface userContextService;
    private final ApiKeyCreationServiceInterface apiKeyCreationService;
    private final ApiKeyUpdateServiceInterface apiKeyUpdateService;
    private final ApiKeyRemovalServiceInterface apiKeyRemovalService;
    private final ApiKeyRetrievalServiceInterface apiKeyRetrievalService;
    private final MessageSource messageSource;

    public ApiKeyController(
            UserContextServiceInterface userContextService,
            ApiKeyCreationServiceInterface apiKeyCreationService,
            ApiKeyUpdateServiceInterface apiKeyUpdateService,
            ApiKeyRemovalServiceInterface apiKeyRemovalService,
            ApiKeyRetrievalServiceInterface apiKeyRetrievalService,
            MessageSource messageSource) {
        this.userContextService = userContextService;
        this.apiKeyCreationService = apiKeyCreationService;
        this.apiKeyUpdateService = apiKeyUpdateService;
        this.apiKeyRemovalService = apiKeyRemovalService;
        this.apiKeyRetrievalService = apiKeyRetrievalService;
        this.messageSource = messageSource;
    }

    /**
     * GET /api/user/api-keys
     * Retrieve all API keys for the current user.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getApiKeys() {
        List<ApiKeyResponse> apiKeyRetrievalResponses =
                apiKeyRetrievalService.getApiKeys(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKeys", apiKeyRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/api-keys
     * Create a new API key.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createApiKey(
            @Valid @RequestBody ApiKeyCreationRequest request) {
        ApiKeyResponse apiKeyCreationResponse = apiKeyCreationService.createApiKey(
                userContextService.getCurrentUserId(),
                request.getPlatformName(),
                request.getApiKeyValue(),
                request.getSecretKey());

        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.add",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKey", apiKeyCreationResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * PUT /api/user/api-keys/{apiKeyId}
     * Update an existing API key.
     */
    @PutMapping("/{apiKeyId}")
    public ResponseEntity<Map<String, Object>> updateApiKey(
            @NotNullField(exception = MissingApiKeyIdException.class)
            @PathVariable
            Integer apiKeyId,
            @Valid @RequestBody ApiKeyUpdateRequest request) {
        ApiKeyResponse apiKeyUpdateResponse = apiKeyUpdateService.updateApiKey(
                userContextService.getCurrentUserId(),
                apiKeyId,
                request.getPlatformName(),
                request.getApiKeyValue(),
                request.getSecretKey());

        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.update",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKey", apiKeyUpdateResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * DELETE /api/user/api-keys/{apiKeyId}
     * Remove an API key.
     */
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<Map<String, Object>> removeApiKey(
            @NotNullField(exception = MissingApiKeyIdException.class)
            @PathVariable
            Integer apiKeyId) {
        apiKeyRemovalService.removeApiKey(userContextService.getCurrentUserId(), apiKeyId);

        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.remove",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }
}