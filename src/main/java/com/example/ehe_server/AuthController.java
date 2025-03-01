package com.example.ehe_server;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", allowCredentials = "true")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            System.out.println("Received login request for email: " + request.email());
            Long userId = authService.validateUser(request.email(), request.password());
            String token = jwtTokenProvider.createToken(userId);

            // Create secure HTTP-only cookie
            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true) // Set to true in production with HTTPS
                    .path("/")
                    .maxAge(Duration.ofHours(1))
                    .sameSite("Strict") // Can be "Strict" for stricter security
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());

            return ResponseEntity.ok().body("Login successful");
        } catch (AuthService.AuthException e) {
            System.out.println("Authentication failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    public record LoginRequest(String email, String password) {}
}
