package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.properties.FrontendProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LogInServiceInterface logInService;
    private final RegistrationServiceInterface registrationService;
    private final RegistrationVerificationServiceInterface registrationVerificationService;
    private final RegistrationVerificationResendServiceInterface registrationVerificationResendService;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final PasswordResetServiceInterface passwordResetService;
    private final EmailChangeVerificationServiceInterface emailChangeVerificationService;
    private final MessageSource messageSource;
    private final UserContextService userContextService;
    private final FrontendProperties frontendProperties;

    public AuthController(
            LogInServiceInterface logInService,
            RegistrationServiceInterface registrationService,
            RegistrationVerificationServiceInterface registrationVerificationService,
            RegistrationVerificationResendServiceInterface registrationVerificationResendService,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            PasswordResetServiceInterface passwordResetService,
            EmailChangeVerificationServiceInterface emailChangeVerificationService,
            MessageSource messageSource,
            UserContextService userContextService,
            FrontendProperties frontendProperties) {
        this.logInService = logInService;
        this.registrationService = registrationService;
        this.registrationVerificationService = registrationVerificationService;
        this.registrationVerificationResendService = registrationVerificationResendService;
        this.passwordResetRequestService = passwordResetRequestService;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.passwordResetService = passwordResetService;
        this.emailChangeVerificationService = emailChangeVerificationService;
        this.messageSource = messageSource;
        this.userContextService = userContextService;
        this.frontendProperties = frontendProperties;
    }

    /**
     * POST /api/auth/login
     * Authenticate user and create session
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        logInService.authenticateUser(request.getEmail(), request.getPassword(), response);

        String successMessage = messageSource.getMessage(
                "success.message.auth.login",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        if (userContextService.getCurrentUserRole().contains("ROLE_ADMIN")) {
            responseBody.put("redirectUrl", frontendProperties.getUrl() + frontendProperties.getAdmin());
        } else {
            String fullUrl = frontendProperties.getUrl() + frontendProperties.getUser().getSuffix();
            responseBody.put("redirectUrl", fullUrl);
        }

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/auth/register
     * Register a new user account
     * Returns 201 Created since a new resource (user) is created
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request) {

        registrationService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        String successMessage = messageSource.getMessage(
                "success.message.auth.registration",
                new Object[]{request.getEmail()},
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("showResendButton", true);

        // 201 Created - a new user resource was created
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * POST /api/auth/verification/resend
     * Resend registration verification email
     */
    @PostMapping("/verification/resend")
    public ResponseEntity<Map<String, Object>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        registrationVerificationResendService.resendVerificationEmail(request.getEmail());

        String successMessage = messageSource.getMessage(
                "success.message.auth.resendVerificationEmail",
                new Object[]{request.getEmail()},
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/auth/verification?token=xxx
     * Verify registration token (typically called from email link)
     */
    @GetMapping("/verification")
    public ResponseEntity<Map<String, Object>> verifyRegistration(@RequestParam String token) {

        registrationVerificationService.verifyRegistrationToken(token);

        String successMessage = messageSource.getMessage(
                "success.message.auth.registrationVerification",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/auth/password/forgot
     * Request password reset email
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        passwordResetRequestService.requestPasswordResetForUnauthenticatedUser(request.getEmail());

        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordResetRequest",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/auth/password/validate?token=xxx
     * Validate password reset token before showing reset form
     */
    @GetMapping("/password/validate")
    public ResponseEntity<Map<String, Object>> validatePasswordResetToken(@RequestParam String token) {

        passwordResetTokenValidationService.validatePasswordResetToken(token);

        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordResetTokenValidation",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/auth/password/reset
     * Reset password with valid token
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request.getToken(), request.getPassword());

        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordReset",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/auth/email/verify?token=xxx
     * Verify email change token
     */
    @GetMapping("/email/verify")
    public ResponseEntity<Map<String, Object>> validateEmailChange(@RequestParam String token) {

        emailChangeVerificationService.validateEmailChange(token);

        String successMessage = messageSource.getMessage(
                "success.message.auth.emailChangeVerification",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }
}