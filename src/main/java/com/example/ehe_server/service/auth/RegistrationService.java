package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.RegistrationServiceInterface;
import com.example.ehe_server.service.intf.email.EmailSenderServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(noRollbackFor = EmailSendFailureException.class)
public class RegistrationService implements RegistrationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSenderServiceInterface emailSenderService;
    private final UserContextService userContextService;
    private final VerificationTokenProperties verificationTokenProperties;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,100}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    public RegistrationService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailSenderServiceInterface emailSenderService,
            UserContextService userContextService,
            VerificationTokenProperties verificationTokenProperties,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailSenderService = emailSenderService;
        this.userContextService = userContextService;
        this.verificationTokenProperties = verificationTokenProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(messageKey = "log.message.auth.registration")
    @Override
    public void registerUser(String username, String email, String password) {

        // Input validation checks (Granular)
        if ((username == null || username.trim().isEmpty()) &&
                (email == null || email.trim().isEmpty()) &&
                (password == null || password.trim().isEmpty())) {
            throw new MissingRegistrationFieldsException();
        }

        if (username == null || username.trim().isEmpty()) {
            throw new MissingUsernameException();
        }

        if (email == null || email.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (password == null || password.trim().isEmpty()) {
            throw new MissingPasswordException();
        }

        // Format validation
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new InvalidUsernameFormatException(username);
        }

        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordFormatException();
        }

        // Database integrity checks
        if (userRepository.findByEmail(email).isPresent()) {
            // We consciously reveal this to help the user log in instead
            throw new EmailAlreadyRegisteredException(email).withActionLink("log in.", "login");
        }

        // Entity construction
        User newUser = new User();
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setAccountStatus(User.AccountStatus.NONVERIFIED);

        User savedUser = userRepository.save(newUser);

        // Audit context setup
        // New users are never Admins immediately, so we hardcode USER role
        userContextService.setUser(String.valueOf(savedUser.getUserId()), "USER");

        // Token generation
        String plainToken = UUID.randomUUID().toString();  // Plain token to send via email
        String hashedToken = passwordEncoder.encode(plainToken);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());

        VerificationToken verificationToken = new VerificationToken(
                savedUser, hashedToken, VerificationToken.TokenType.REGISTRATION, expiryDate);
        verificationTokenRepository.save(verificationToken);

        // Email notification
        try {
            emailSenderService.sendRegistrationVerificationEmail(savedUser, plainToken, email);
        } catch (EmailSendFailureException e) {
            // This runtime exception triggers the rollback of the User creation, make sure to forward it with resend button
            throw new EmailSendFailureException(email, e.getMessage()).withResendButton();
        }
    }
}