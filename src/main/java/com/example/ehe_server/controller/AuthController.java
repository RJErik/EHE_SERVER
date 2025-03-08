package com.example.ehe_server.controller;

import com.example.ehe_server.dto.LoginRequest;
import com.example.ehe_server.entities.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.JwtTokenValidatorInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final JwtTokenValidatorInterface jwtTokenValidator;

    @Value("${jwt.expiration.time}")
    private long jwtExpirationTime;

    public AuthController(UserRepository userRepository,
                          JwtTokenGeneratorInterface jwtTokenGenerator,
                          JwtTokenValidatorInterface jwtTokenValidator) {
        this.userRepository = userRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Hash the email for lookup since emails are stored as hashes
            String emailHash = hashEmail(request.getEmail());

            // Find user by email hash
            Optional<User> userOpt = userRepository.findByEmailHash(emailHash);

            if (userOpt.isEmpty()) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password");
                return ResponseEntity.ok(responseBody);
            }

            User user = userOpt.get();

            // Check account status
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                responseBody.put("success", false);
                responseBody.put("message", "Your account is not active");
                return ResponseEntity.ok(responseBody);
            }

            // Verify password (assuming BCrypt is used)
            if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
                responseBody.put("success", false);
                responseBody.put("message", "Invalid email or password");
                return ResponseEntity.ok(responseBody);
            }

            // Generate JWT token
            String jwtToken = jwtTokenGenerator.generateToken(Long.valueOf(user.getUserId()));

            // Create secure, HttpOnly cookie
            Cookie cookie = new Cookie("jwt_token", jwtToken);
            cookie.setHttpOnly(true); // Makes the cookie inaccessible to JavaScript
            cookie.setSecure(true);   // Only transmitted over HTTPS
            cookie.setPath("/");      // Available across the entire site
            cookie.setMaxAge((int) (jwtExpirationTime / 1000)); // In seconds
            cookie.setAttribute("SameSite", "Strict");

            // Add cookie to response
            response.addCookie(cookie);

            // Return success response
            responseBody.put("success", true);
            responseBody.put("message", "Login successful");
            responseBody.put("userName", user.getUserName());

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("message", "An error occurred during login");
            return ResponseEntity.status(500).body(responseBody);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Clear JWT cookie
        Cookie cookie = new Cookie("jwt_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }

    // Helper method to hash email
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash email", e);
        }
    }
}
