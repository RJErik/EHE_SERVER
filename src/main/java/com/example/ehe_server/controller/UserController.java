package com.example.ehe_server.controller;

import com.example.ehe_server.dto.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.user.*;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
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

    public UserController(UserValidationServiceInterface userValidationService,
                          UserLogoutServiceInterface userLogoutService,
                          AuditContextService auditContextService,
                          UserContextServiceInterface userContextService,
                          UserRepository userRepository,
                          PasswordResetRequestServiceInterface passwordResetRequestService,
                          JwtTokenRenewalServiceInterface jwtTokenRenewalService,
                          UserInfoServiceInterface userInfoService,
                          UserDeactivationServiceInterface userDeactivationService,
                          EmailChangeRequestServiceInterface emailChangeRequestService) {
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
}
