package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class RegistrationService implements RegistrationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository; // Inject token repo
    private final EmailServiceInterface emailService; // Inject email service
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;

    @Value("${app.verification.token.expiry-hours}") // Get expiry from config
    private int tokenExpiryHours;

    // Validation patterns remain the same...
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");


    public RegistrationService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository, // Add to constructor
            EmailServiceInterface emailService, // Add to constructor
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository; // Assign
        this.emailService = emailService; // Assign
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public void registerUser(String username, String email, String password) {
        // Validation logic remains the same...
        // Validate input fields
        if (username == null || email == null || password == null ||
                username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            throw new MissingRegistrationFieldsException();
        }
        // Validate username format
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new InvalidUsernameFormatException(username);
        }
        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }
        // Validate password strength
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordFormatException();
        }


        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email).withActionLink("log in.", "login");
        }

        // Create new user entity
        User newUser = new User();
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        newUser.setAccountStatus(User.AccountStatus.NONVERIFIED); // <<<--- SET STATUS TO NONVERIFIED
        newUser.setRegistrationDate(LocalDateTime.now());

        // Save the user (audit columns like created_by will be set by trigger)
        User savedUser = userRepository.save(newUser);

        // Now that user is created, update the audit context for subsequent actions in this transaction
        // Update audit context
        String userIdStr = String.valueOf(savedUser.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(savedUser.getUserId());

        // Set audit context
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(userIdStr, role);

        // --- NEW: Generate and save verification token ---
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
        VerificationToken verificationToken = new VerificationToken(savedUser, token, VerificationToken.TokenType.REGISTRATION, expiryDate);
        verificationTokenRepository.save(verificationToken);
        // -------------------------------------------------

        // Log successful registration attempt (pending verification)
        loggingService.logAction("User registered successfully (pending verification). Token generated.");

        // --- REMOVED: JWT Generation and Cookie Setting ---
        // No automatic login on registration anymore
        // --------------------------------------------------

        try {
            // Call the *synchronous* version of the email sending method
            emailService.sendVerificationEmail(savedUser, token, email);

            // If email sending was successful (no exception thrown), set the success response for the controller
            loggingService.logAction("Sent verification email to " + email);

        } catch (MailException e) {
            // Set the response to indicate failure *because* the email couldn't be sent
            loggingService.logError("Failed to send verification email to " + email, e);
            throw new EmailSendFailureException(email, e.getMessage()).withResendButton();

            // IMPORTANT: Throw a runtime exception to trigger Transaction rollback.
            // The user record created above should be rolled back if we can't send the email.
        }
    }
}