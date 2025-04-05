package com.example.ehe_server.controller;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.dto.RegistrationRequest;
import com.example.ehe_server.dto.ResendVerificationRequest; // Import new DTO
import com.example.ehe_server.service.intf.auth.LogInServiceInterface; // Assuming separate interface for login
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import com.example.ehe_server.service.intf.auth.VerificationServiceInterface; // Import new service interface
import jakarta.servlet.http.HttpServletResponse;
// Removed Transactional from controller
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus; // For status codes
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.net.URI; // Potentially for redirect example
import java.net.URLEncoder; // Potentially for redirect example
import java.nio.charset.StandardCharsets; // Potentially for redirect example


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LogInServiceInterface authenticationService;
    private final RegistrationServiceInterface registrationService;
    private final VerificationServiceInterface verificationService;

    public AuthController(
            LogInServiceInterface authenticationService,
            RegistrationServiceInterface registrationService,
            VerificationServiceInterface verificationService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.verificationService = verificationService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        Map<String, Object> responseBody = authenticationService.authenticateUser(request, response);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        // Return 401 Unauthorized if login fails, otherwise 200 OK
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request,
                                                        HttpServletResponse response) {
        Map<String, Object> responseBody = registrationService.registerUser(request, response);
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        // Returns 200 OK on success (pending verification) or 400 Bad Request on validation/duplicate error
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authenticationService.logoutUser(response);
        return ResponseEntity.ok().build();
    }

    // --- Endpoint for Resend ---
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        Map<String, Object> responseBody = verificationService.resendVerification(request.getEmail());

        boolean rateLimited = "RATE_LIMITED".equals(responseBody.get("status"));
        boolean success = (boolean) responseBody.getOrDefault("success", false);

        if (rateLimited) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(responseBody);
        } else if (success) {
            return ResponseEntity.ok(responseBody);
        } else {
            // Determine if it was "user not found" or other user error -> 400/404?
            // For now, defaulting to 400 for client-side errors other than rate limit
            return ResponseEntity.badRequest().body(responseBody);
        }
    }

    // --- Endpoint for Verification ---
    @GetMapping("/verify") // Using GET as it's typically triggered by clicking a link
    public ResponseEntity<Map<String,Object>> verifyAccount(@RequestParam("token") String token) {
        Map<String, Object> responseBody = verificationService.verifyToken(token);
        boolean success = (boolean) responseBody.getOrDefault("success", false);

        // Just return JSON for frontend processing
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}