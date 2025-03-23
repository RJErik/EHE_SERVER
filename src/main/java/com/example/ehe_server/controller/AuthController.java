package com.example.ehe_server.controller;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.dto.RegistrationRequest;
import com.example.ehe_server.service.intf.auth.LoggingServiceInterface;
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoggingServiceInterface authenticationService;
    private final RegistrationServiceInterface registrationService;

    public AuthController(
            LoggingServiceInterface authenticationService,
            RegistrationServiceInterface registrationService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        Map<String, Object> responseBody = authenticationService.authenticateUser(request, response);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request,
                                                        HttpServletResponse response) {
        Map<String, Object> responseBody = registrationService.registerUser(request, response);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authenticationService.logoutUser(response);
        return ResponseEntity.ok().build();
    }
}
