package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.auth.*;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.ehe_server.dto.PasswordResetRequest;
import com.example.ehe_server.dto.NewPasswordRequest;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetServiceInterface;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LogInServiceInterface authenticationService;
    private final RegistrationServiceInterface registrationService;
    private final RegistrationVerificationServiceInterface verificationService;
    private final EmailServiceInterface emailService;
    private final HashingServiceInterface hashingService; // Needed here for lookup
    private final UserRepository userRepository; // Needed here for lookup
    private final PasswordResetRequestServiceInterface passwordResetRequestService;
    private final PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService;
    private final PasswordResetServiceInterface passwordResetService;

    public AuthController(
            LogInServiceInterface authenticationService,
            RegistrationServiceInterface registrationService,
            RegistrationVerificationServiceInterface verificationService,
            EmailServiceInterface emailService,
            HashingServiceInterface hashingService, // Inject HashingService
            UserRepository userRepository,
            PasswordResetRequestServiceInterface passwordResetRequestService,
            PasswordResetTokenValidationServiceInterface passwordResetTokenValidationService,
            PasswordResetServiceInterface passwordResetService) { // Inject UserRepository
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.verificationService = verificationService;
        this.emailService = emailService;
        this.hashingService = hashingService;
        this.userRepository = userRepository;
        this.passwordResetRequestService = passwordResetRequestService;
        this.passwordResetTokenValidationService = passwordResetTokenValidationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        Map<String, Object> responseBody = authenticationService.authenticateUser(request, response);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request,
                                                        HttpServletResponse response) {
        Map<String, Object> responseBody = registrationService.registerUser(request, response);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authenticationService.logoutUser(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        String email = request.getEmail();

        // --- Hashing and User Lookup moved to Controller ---
        String emailHash = hashingService.hashEmail(email);
        Optional<User> userOpt = userRepository.findByEmailHash(emailHash);

        if (userOpt.isEmpty()) {
            // User not found based on the provided email hash
            Map<String, Object> responseBody = Map.of(
                    "success", false,
                    "message", "User not found with the provided email."
            );
            return ResponseEntity.badRequest().body(responseBody);
        }
        // --- End of Controller-specific lookup ---

        User user = userOpt.get();

        // Call EmailService with the found User object and original email
        Map<String, Object> responseBody = emailService.resendVerificationEmail(user, email);

        // Process response from EmailService (rate limit, success, other errors)
        String status = (String) responseBody.get("status");

        if ("RATE_LIMITED".equals(status)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(responseBody);
        } else if ("RESEND_SUCCESS".equals(status)) {
            return ResponseEntity.ok(responseBody);
        } else {
            // Handle other errors returned by EmailService (e.g., already verified, suspended)
            return ResponseEntity.badRequest().body(responseBody);
        }
    }

    @GetMapping("/verify_registration")
    public ResponseEntity<Map<String, Object>> verifyAccount(@RequestParam("token") String token) {
        Map<String, Object> responseBody = verificationService.verifyRegistrationToken(token);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    // Add these endpoints to AuthController class
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        Map<String, Object> responseBody = passwordResetRequestService.requestPasswordReset(request.getEmail());
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam("token") String token) {
        Map<String, Object> responseBody = passwordResetTokenValidationService.validatePasswordResetToken(token);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        Map<String, Object> responseBody = passwordResetService.resetPassword(request.getToken(), request.getPassword());
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

}