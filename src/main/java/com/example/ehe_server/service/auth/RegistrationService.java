// src/main/java/com/example/ehe_server/service/auth/RegistrationService.java
package com.example.ehe_server.service.auth;

import com.example.ehe_server.dto.RegistrationRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.HashingServiceInterface;
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.security.JwtTokenGeneratorInterface;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class RegistrationService implements RegistrationServiceInterface {

    private final UserRepository userRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final HashingServiceInterface hashingService;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    // Username validation pattern (alphanumeric and underscore only, minimum 3 characters)
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,}$");

    // Password validation (min 8 characters, at least 1 letter and 1 number)
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    public RegistrationService(
            UserRepository userRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            HashingServiceInterface hashingService,
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.hashingService = hashingService;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> registerUser(RegistrationRequest request, HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        try {
            // Validate input fields
            if (request.getUsername() == null || request.getEmail() == null || request.getPassword() == null) {
                responseBody.put("success", false);
                responseBody.put("message", "All fields are required");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Registration failed: Missing required fields");
                return responseBody;
            }

            // Validate username format
            if (!USERNAME_PATTERN.matcher(request.getUsername()).matches()) {
                responseBody.put("success", false);
                responseBody.put("message", "Username must be at least 3 characters and contain only letters, numbers, and underscores");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Registration failed: Invalid username format");
                return responseBody;
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                responseBody.put("success", false);
                responseBody.put("message", "Please enter a valid email address");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Registration failed: Invalid email format");
                return responseBody;
            }

            // Validate password strength
            if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
                responseBody.put("success", false);
                responseBody.put("message", "Password must be at least 8 characters with at least one letter and one number");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Registration failed: Password does not meet strength requirements");
                return responseBody;
            }

            // Check if email already exists
            String emailHash = hashingService.hashEmail(request.getEmail());
            if (userRepository.findByEmailHash(emailHash).isPresent()) {
                responseBody.put("success", false);
                responseBody.put("message", "Email is already registered");
                loggingService.logAction(null, auditContextService.getCurrentUser(), "Registration failed: Email already exists");
                return responseBody;
            }

            // Create new user entity
            User newUser = new User();
            newUser.setUserName(request.getUsername());
            newUser.setEmailHash(emailHash);
            newUser.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
            newUser.setAccountStatus(User.AccountStatus.ACTIVE);
            newUser.setRegistrationDate(LocalDateTime.now());

            // Save the user
            User savedUser = userRepository.save(newUser);

            // Now that user is created, update the audit context
            auditContextService.setCurrentUser(String.valueOf(savedUser.getUserId()));
            auditContextService.setCurrentUserRole("USER");

            // Log successful registration
            loggingService.logAction(Integer.parseInt(auditContextService.getCurrentUser()), auditContextService.getCurrentUser(), "User registered successfully");

            // Create roles list for JWT
            List<String> roles = new ArrayList<>();
            roles.add("USER");

            // Generate JWT token
            String jwtToken = jwtTokenGenerator.generateToken(Long.valueOf(savedUser.getUserId()), roles);

            // Create JWT cookie
            cookieService.createJwtCookie(jwtToken, response);

            // Return success response
            responseBody.put("success", true);
            responseBody.put("message", "Registration successful");
            responseBody.put("userName", savedUser.getUserName());
            responseBody.put("roles", roles);

            return responseBody;

        } catch (Exception e) {
            responseBody.put("success", false);
            responseBody.put("message", "An error occurred during registration");

            // Log the error
            loggingService.logError(null, "unknown", "Error during registration: " + e.getMessage(), e);

            return responseBody;
        }
    }
}
