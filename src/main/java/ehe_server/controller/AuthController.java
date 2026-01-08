package ehe_server.controller;

import ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.dto.*;
import ehe_server.dto.PasswordResetRequest;
import ehe_server.dto.RegistrationRequest;
import ehe_server.dto.ResendVerificationRequest;
import ehe_server.dto.ResetPasswordRequest;
import ehe_server.exception.custom.MissingVerificationTokenException;
import com.example.ehe_server.service.intf.auth.*;
import ehe_server.service.intf.auth.*;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final RegistrationServiceInterface registrationService;
    private final RegistrationVerificationServiceInterface registrationVerificationService;
    private final RegistrationVerificationResendServiceInterface registrationVerificationResendService;
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final PasswordResetServiceInterface passwordResetService;
    private final EmailChangeVerificationServiceInterface emailChangeVerificationService;
    private final MessageSource messageSource;

    public AuthController(
            RegistrationServiceInterface registrationService,
            RegistrationVerificationServiceInterface registrationVerificationService,
            RegistrationVerificationResendServiceInterface registrationVerificationResendService,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            PasswordResetServiceInterface passwordResetService,
            EmailChangeVerificationServiceInterface emailChangeVerificationService,
            MessageSource messageSource) {
        this.registrationService = registrationService;
        this.registrationVerificationService = registrationVerificationService;
        this.registrationVerificationResendService = registrationVerificationResendService;
        this.passwordResetRequestService = passwordResetRequestService;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.passwordResetService = passwordResetService;
        this.emailChangeVerificationService = emailChangeVerificationService;
        this.messageSource = messageSource;
    }

    /**
     * POST /api/auth/users
     * Register a new user account.
     */
    @PostMapping("/users")
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

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * POST /api/auth/verification-requests
     * Request a new verification email to be sent.
     */
    @PostMapping("/verification-requests")
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

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * GET /api/auth/registrations/{token}
     * Verify and complete registration using the token.
     */
    @GetMapping("/registrations/{token}")
    public ResponseEntity<Map<String, Object>> verifyRegistration(
            @NotEmptyString(exception = MissingVerificationTokenException.class)
            @PathVariable
            String token) {

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
     * POST /api/auth/password-reset-requests
     * Request a password reset email.
     */
    @PostMapping("/password-reset-requests")
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

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * GET /api/auth/password-reset-tokens/{token}
     * Retrieve and validate a password reset token.
     */
    @GetMapping("/password-reset-tokens/{token}")
    public ResponseEntity<Map<String, Object>> validatePasswordResetToken(
            @NotEmptyString(exception = MissingVerificationTokenException.class)
            @PathVariable
            String token) {

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
     * POST /api/auth/password-resets
     * Execute a password reset with a valid token.
     */
    @PostMapping("/password-resets")
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
     * GET /api/auth/email-verifications/{token}
     * Verify and complete an email change using the token.
     */
    @GetMapping("/email-verifications/{token}")
    public ResponseEntity<Map<String, Object>> validateEmailChange(
            @NotEmptyString(exception = MissingVerificationTokenException.class)
            @PathVariable
            String token) {

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