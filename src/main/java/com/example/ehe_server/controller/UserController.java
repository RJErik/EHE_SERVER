package com.example.ehe_server.controller;

import com.example.ehe_server.dto.ApiKeyAddRequest;
import com.example.ehe_server.dto.ApiKeyDeleteRequest;
import com.example.ehe_server.dto.ApiKeyUpdateRequest;
import com.example.ehe_server.dto.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.user.ApiKeyAddServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyDeleteServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyListServiceInterface;
import com.example.ehe_server.service.intf.user.ApiKeyUpdateServiceInterface;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.user.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserValidationServiceInterface userValidationService;
    private final UserLogoutServiceInterface userLogoutService;
    private final AuditContextService auditContextService;
    private final UserContextServiceInterface userContextService;
    private final UserRepository userRepository;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final JwtTokenRenewalServiceInterface jwtTokenRenewalService;
    private final UserInfoServiceInterface userInfoService;
    private final UserDeactivationServiceInterface userDeactivationService;
    private final EmailChangeRequestServiceInterface emailChangeRequestService;
    private final ApiKeyAddServiceInterface apiKeyAddService;
    private final ApiKeyUpdateServiceInterface apiKeyUpdateService;
    private final ApiKeyDeleteServiceInterface apiKeyDeleteService;
    private final ApiKeyListServiceInterface apiKeyListService;

    public UserController(
            UserValidationServiceInterface userValidationService,
            UserLogoutServiceInterface userLogoutService,
            AuditContextService auditContextService,
            UserContextServiceInterface userContextService,
            UserRepository userRepository,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            JwtTokenRenewalServiceInterface jwtTokenRenewalService,
            UserInfoServiceInterface userInfoService,
            UserDeactivationServiceInterface userDeactivationService,
            EmailChangeRequestServiceInterface emailChangeRequestService,
            ApiKeyAddServiceInterface apiKeyAddService,
            ApiKeyUpdateServiceInterface apiKeyUpdateService,
            ApiKeyDeleteServiceInterface apiKeyDeleteService,
            ApiKeyListServiceInterface apiKeyListService) {
        this.userValidationService = userValidationService;
        this.userLogoutService = userLogoutService;
        this.auditContextService = auditContextService;
        this.userContextService = userContextService;
        this.userRepository = userRepository;
        this.passwordResetRequestService = passwordResetRequestService;
        this.jwtTokenRenewalService = jwtTokenRenewalService;
        this.userInfoService = userInfoService;
        this.userDeactivationService = userDeactivationService;
        this.emailChangeRequestService = emailChangeRequestService;
        this.apiKeyAddService = apiKeyAddService;
        this.apiKeyUpdateService = apiKeyUpdateService;
        this.apiKeyDeleteService = apiKeyDeleteService;
        this.apiKeyListService = apiKeyListService;
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUser() {
        // Setup the user context from Spring Security - this sets the PostgreSQL context
        userContextService.setupUserContext();

        // Verify user status
        Map<String, Object> response = userValidationService.verifyUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call user info service
        Map<String, Object> responseBody = userInfoService.getUserInfo(userId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        // Setup the user context
        userContextService.setupUserContext();

        // Call logout service
        Map<String, Object> responseBody = userLogoutService.logoutUser(response);

        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        Map<String, Object> response = new HashMap<>();

        String userIdStr = auditContextService.getCurrentUser();
        // Convert string ID to integer
        Integer userId = Integer.parseInt(userIdStr);

        // Find the user by ID
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }

        User user = userOpt.get();
        String email = user.getEmail();

        // Call the password reset service with the user's email
        Map<String, Object> responseBody = passwordResetRequestService.requestPasswordReset(email);

        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/renew-token")
    public ResponseEntity<Map<String, Object>> renewToken(HttpServletResponse response) {
        // Setup the user context
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call token renewal service
        Map<String, Object> responseBody = jwtTokenRenewalService.renewToken(userId, response);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateAccount() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call deactivation service
        Map<String, Object> responseBody = userDeactivationService.deactivateUser(userId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/change-email")
    public ResponseEntity<Map<String, Object>> requestEmailChange(@Valid @RequestBody EmailChangeRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call email change request service
        Map<String, Object> responseBody = emailChangeRequestService.requestEmailChange(userId, request.getNewEmail());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    // API Key Management Endpoints

    @GetMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> listApiKeys() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call API key list service
        Map<String, Object> responseBody = apiKeyListService.listApiKeys(userId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> addApiKey(@Valid @RequestBody ApiKeyAddRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call API key add service with secret key
        Map<String, Object> responseBody = apiKeyAddService.addApiKey(
                userId, request.getPlatformName(), request.getApiKeyValue(), request.getSecretKey());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PutMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> updateApiKey(@Valid @RequestBody ApiKeyUpdateRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call API key update service with secret key
        Map<String, Object> responseBody = apiKeyUpdateService.updateApiKey(
                userId, request.getApiKeyId(), request.getPlatformName(),
                request.getApiKeyValue(), request.getSecretKey());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @DeleteMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@Valid @RequestBody ApiKeyDeleteRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Get the current user ID from the audit context
        Long userId = Long.parseLong(auditContextService.getCurrentUser());

        // Call API key delete service
        Map<String, Object> responseBody = apiKeyDeleteService.deleteApiKey(userId, request.getApiKeyId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
