package com.example.ehe_server.controller;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.service.intf.auth.AuthenticationServiceInterface;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationServiceInterface authenticationService;

    public AuthController(AuthenticationServiceInterface authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        Map<String, Object> responseBody = authenticationService.authenticateUser(request, response);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authenticationService.logoutUser(response);
        return ResponseEntity.ok().build();
    }
}
