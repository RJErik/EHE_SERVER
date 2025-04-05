package com.example.ehe_server.service.auth;

import com.example.ehe_server.dto.RegistrationRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository; // Import new repository
import com.example.ehe_server.service.audit.AuditContextService;
// Removed unused cookie/jwt imports for registration
import com.example.ehe_server.service.intf.auth.HashingServiceInterface;
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import com.example.ehe_server.service.intf.email.EmailServiceInterface; // Import email service
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
// Import JWT if needed elsewhere, but not for register response anymore
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value; // For expiry config
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // For token generation
import java.util.regex.Pattern;

@Service
public class RegistrationService implements RegistrationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository; // Inject token repo
    private final HashingServiceInterface hashingService;
    private final EmailServiceInterface emailService; // Inject email service
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    @Value("${app.verification.token.expiry-hours}") // Get expiry from config
    private long tokenExpiryHours;

    // Validation patterns remain the same...
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");


    public RegistrationService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository, // Add to constructor
            HashingServiceInterface hashingService,
            EmailServiceInterface emailService, // Add to constructor
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository; // Assign
        this.hashingService = hashingService;
        this.emailService = emailService; // Assign
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> registerUser(RegistrationRequest request, HttpServletResponse response) {
        Map<String, Object> responseBody = new HashMap<>();

        // Validation logic remains the same...
        try {
            // Validate input fields
            if (request.getUsername() == null || request.getEmail() == null || request.getPassword() == null ||
                    request.getUsername().isBlank() || request.getEmail().isBlank() || request.getPassword().isBlank()) { // Added isBlank checks
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
            newUser.setAccountStatus(User.AccountStatus.NONVERIFIED); // <<<--- SET STATUS TO NONVERIFIED
            newUser.setRegistrationDate(LocalDateTime.now());

            // Save the user (audit columns like created_by will be set by trigger)
            User savedUser = userRepository.save(newUser);

            // Now that user is created, update the audit context for subsequent actions in this transaction
            auditContextService.setCurrentUser(String.valueOf(savedUser.getUserId()));
            // Role is not fully established yet, could set to 'NONVERIFIED' or keep as 'USER' contextually
            auditContextService.setCurrentUserRole("USER_PENDING_VERIFICATION");

            // --- NEW: Generate and save verification token ---
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
            VerificationToken verificationToken = new VerificationToken(savedUser, token, VerificationToken.TokenType.REGISTRATION, expiryDate);
            verificationTokenRepository.save(verificationToken);
            // -------------------------------------------------

            // Log successful registration attempt (pending verification)
            loggingService.logAction(savedUser.getUserId(), String.valueOf(savedUser.getUserId()), "User registered successfully (pending verification). Token generated.");

            // --- NEW: Send verification email ---
            // Pass the original email, not the hash
            emailService.sendVerificationEmail(savedUser, token, request.getEmail());
            // ------------------------------------

            // --- REMOVED: JWT Generation and Cookie Setting ---
            // No automatic login on registration anymore
            // --------------------------------------------------

            // Return success response indicating verification is needed
            responseBody.put("success", true);
            responseBody.put("message", "Registration successful. Please check your email (" + request.getEmail() + ") to verify your account.");
            // DO NOT return user details or roles here

            return responseBody;

        } catch (Exception e) {
            // Log the error with potentially available audit context
            String currentUserCtx = "unknown";
            try { currentUserCtx = auditContextService.getCurrentUser(); } catch (Exception ignored) {}

            loggingService.logError(null, currentUserCtx, "Error during registration: " + e.getMessage(), e);

            responseBody.put("success", false);
            responseBody.put("message", "An error occurred during registration. Please try again later.");
            return responseBody;
        }
    }
}