package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.intf.user.ApiKeyCreationServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyRemovalServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyRetrievalServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyUpdateServiceInterface;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.user.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserValidationServiceInterface userValidationService;
    private final UserLogoutServiceInterface userLogoutService;
    private final UserContextServiceInterface userContextService;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final JwtTokenRenewalServiceInterface jwtTokenRenewalService;
    private final UserInfoServiceInterface userInfoService;
    private final UserDeactivationServiceInterface userDeactivationService;
    private final EmailChangeRequestServiceInterface emailChangeRequestService;
    private final ApiKeyCreationServiceInterface apiKeyCreationService;
    private final ApiKeyUpdateServiceInterface apiKeyUpdateService;
    private final ApiKeyRemovalServiceInterface apiKeyRemovalService;
    private final ApiKeyRetrievalServiceInterface apiKeyRetrievalService;
    private final MessageSource messageSource;

    public UserController(
            UserValidationServiceInterface userValidationService,
            UserLogoutServiceInterface userLogoutService,
            UserContextServiceInterface userContextService,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            JwtTokenRenewalServiceInterface jwtTokenRenewalService,
            UserInfoServiceInterface userInfoService,
            UserDeactivationServiceInterface userDeactivationService,
            EmailChangeRequestServiceInterface emailChangeRequestService,
            ApiKeyCreationServiceInterface apiKeyCreationService,
            ApiKeyUpdateServiceInterface apiKeyUpdateService,
            ApiKeyRemovalServiceInterface apiKeyRemovalService,
            ApiKeyRetrievalServiceInterface apiKeyRetrievalService,
            MessageSource messageSource) {
        this.userValidationService = userValidationService;
        this.userLogoutService = userLogoutService;
        this.userContextService = userContextService;
        this.passwordResetRequestService = passwordResetRequestService;
        this.jwtTokenRenewalService = jwtTokenRenewalService;
        this.userInfoService = userInfoService;
        this.userDeactivationService = userDeactivationService;
        this.emailChangeRequestService = emailChangeRequestService;
        this.apiKeyCreationService = apiKeyCreationService;
        this.apiKeyUpdateService = apiKeyUpdateService;
        this.apiKeyRemovalService = apiKeyRemovalService;
        this.apiKeyRetrievalService = apiKeyRetrievalService;
        this.messageSource = messageSource;
    }

    @GetMapping("/verify-user")
    public ResponseEntity<Map<String, Object>> verifyUser() {
        // Call automated trade rule retrieval service
        userValidationService.verifyUser();

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.verifyUser", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OUT!!!!!!!!!!!!!!!!! I think it is done
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        // Call automated trade rule retrieval service
        UserInfoResponse userInfoResponse = userInfoService.getUserInfo(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.userInfo.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("userInfo", userInfoResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        // Call automated trade rule retrieval service
        userLogoutService.logoutUser(userContextService.getCurrentUserId().intValue(), response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.logout", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset() {

        passwordResetRequestService.requestPasswordResetForAuthenticatedUser(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordResetRequest", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/renew-token")
    public ResponseEntity<Map<String, Object>> renewToken(HttpServletResponse response) {
        // Call automated trade rule retrieval service
        jwtTokenRenewalService.renewToken(userContextService.getCurrentUserId(), response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.jwtTokenRenewal", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/deactivate-account")
    public ResponseEntity<Map<String, Object>> deactivateAccount() {
        // Call automated trade rule retrieval service
        userDeactivationService.deactivateUser(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.userDeactivation", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/change-email")
    public ResponseEntity<Map<String, Object>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        // Call automated trade rule retrieval service
        emailChangeRequestService.requestEmailChange(userContextService.getCurrentUserId(), request.getNewEmail());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.emailChangeRequest", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("showResendButton", true);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    // API Key Management Endpoints
    @GetMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> getApiKeys() {
        // Call automated trade rule retrieval service
        List<ApiKeyRetrievalResponse> apiKeyRetrievalResponses = apiKeyRetrievalService.getApiKeys(userContextService.getCurrentUserId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKeys", apiKeyRetrievalResponses);


        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OVER!!!!!!!!!!!!!!!!!!
    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> createApiKey(@Valid @RequestBody ApiKeyCreationRequest request) {
        // Call automated trade rule retrieval service
        ApiKeyCreationResponse apiKeyCreationResponse = apiKeyCreationService.createApiKey(
                userContextService.getCurrentUserId(), request.getPlatformName(), request.getApiKeyValue(), request.getSecretKey());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.add", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKey", apiKeyCreationResponse);


        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PutMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> updateApiKey(@Valid @RequestBody ApiKeyUpdateRequest request) {
        // Call automated trade rule retrieval service
        ApiKeyUpdateResponse apiKeyUpdateResponse = apiKeyUpdateService.updateApiKey(
                userContextService.getCurrentUserId(), request.getApiKeyId(), request.getPlatformName(),
                request.getApiKeyValue(), request.getSecretKey());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.update", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("apiKey", apiKeyUpdateResponse);


        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @DeleteMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> removeApiKey(@Valid @RequestBody ApiKeyRemoveRequest request) {
        // Call automated trade rule retrieval service
        apiKeyRemovalService.removeApiKey(userContextService.getCurrentUserId(), request.getApiKeyId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.user.apiKey.remove", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}
