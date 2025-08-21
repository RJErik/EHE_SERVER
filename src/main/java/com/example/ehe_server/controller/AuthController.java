package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.*;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ehe_server.dto.PasswordResetRequest;
import com.example.ehe_server.dto.ResetPasswordRequest;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetServiceInterface;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LogInServiceInterface logInService;
    private final RegistrationServiceInterface registrationService;
    private final RegistrationVerificationServiceInterface registrationVerificationServiceInterface;
    private final EmailServiceInterface emailService;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final PasswordResetServiceInterface passwordResetService;
    private final EmailChangeVerificationServiceInterface emailChangeVerificationService;
    private final MessageSource messageSource;
    private final UserContextService userContextService;

    public AuthController(
            LogInServiceInterface logInService,
            RegistrationServiceInterface registrationService,
            RegistrationVerificationServiceInterface registrationVerificationServiceInterface,
            EmailServiceInterface emailService,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            PasswordResetServiceInterface passwordResetService,
            EmailChangeVerificationServiceInterface emailChangeVerificationService,
            MessageSource messageSource,
            UserContextService userContextService) {
        this.logInService = logInService;
        this.registrationService = registrationService;
        this.registrationVerificationServiceInterface = registrationVerificationServiceInterface;
        this.emailService = emailService;
        this.passwordResetRequestService = passwordResetRequestService;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.passwordResetService = passwordResetService;
        this.emailChangeVerificationService = emailChangeVerificationService;
        this.messageSource = messageSource;
        this.userContextService = userContextService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        // Call alert retrieval service
        logInService.authenticateUser(request, response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.login", // The key from your properties file
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request,
                                                        HttpServletResponse response) {
        // Call alert retrieval service
        registrationService.registerUser(request, response);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.registration", // The key from your properties file
                new Object[]{request.getEmail()},                // Arguments for the message (none in this case)
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

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        String email = request.getEmail();
        // Call alert retrieval service
        emailService.resendVerificationEmail(userContextService.getCurrentHumanUser(), email);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.resendVerificationEmail", // The key from your properties file
                new Object[]{request.getEmail()},                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);

    }

    @GetMapping("/verify_registration")
    public ResponseEntity<Map<String, Object>> verifyRegistration(@RequestParam("token") String token) {
        // Call alert retrieval service
        registrationVerificationServiceInterface.verifyRegistrationToken(token);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.registrationVerification", // The key from your properties file
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

    // Add these endpoints to AuthController class
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        // Call alert retrieval service
        passwordResetRequestService.requestPasswordResetForUnauthenticatedUser(request.getEmail());

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

    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validatePasswordResetToken(@RequestParam("token") String token) {
        // Call alert retrieval service
        passwordResetTokenValidationService.validatePasswordResetToken(token);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordResetTokenValidation", // The key from your properties file
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

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // Call alert retrieval service
        passwordResetService.resetPassword(request.getToken(), request.getPassword());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.passwordReset", // The key from your properties file
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

    @GetMapping("/verify-email-change")
    public ResponseEntity<Map<String, Object>> validateEmailChange(@RequestParam("token") String token) {
        // Call alert retrieval service
        emailChangeVerificationService.validateEmailChange(token);

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.auth.emailChangeVerification", // The key from your properties file
                new Object[]{token},                // Arguments for the message (none in this case)
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