package com.example.ehe_server.service.user;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.EmailChangeRequestRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.email.EmailSenderServiceInterface;
import com.example.ehe_server.service.intf.user.EmailChangeRequestServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class EmailChangeRequestService implements EmailChangeRequestServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 3;
    private static final int RATE_LIMIT_MINUTES = 30;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final EmailSenderServiceInterface emailSenderService;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;
    private final VerificationTokenProperties verificationTokenProperties;
    private final PasswordEncoder passwordEncoder;

    public EmailChangeRequestService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            EmailSenderServiceInterface emailSenderService,
            UserContextService userContextService,
            AdminRepository adminRepository,
            VerificationTokenProperties verificationTokenProperties,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.emailSenderService = emailSenderService;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
        this.verificationTokenProperties = verificationTokenProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(messageKey = "log.message.user.emailChangeRequest")
    @Override
    public void requestEmailChange(Integer userId, String newEmail) {
        // Validate email format
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (newEmail.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(newEmail).matches()) {
            throw new InvalidEmailFormatException(newEmail);
        }

        // Find the user
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        User user = userOpt.get();

        // Set audit context
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Check if account is active
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new InactiveAccountException(userId.toString(), user.getAccountStatus().name());
        }

        // Check if the new email is the same as the current one
        if (user.getEmail().equals(newEmail)) {
            throw new SameEmailChangeException();
        }

        // Check if the new email is already in use by another user
        Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
        if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getUserId().equals(user.getUserId())) {
            throw new EmailAlreadyInUseException(newEmail);
        }

        // Rate limiting check
        LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                user.getUserId(), VerificationToken.TokenType.EMAIL_CHANGE, rateLimitThreshold);

        if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
            throw new EmailChangeRateLimitExceededException(RATE_LIMIT_MINUTES).withResendButton();
        }

        // Create new token
        String plainToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(plainToken);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());
        VerificationToken newToken = new VerificationToken(
                user, hashedToken, VerificationToken.TokenType.EMAIL_CHANGE, expiryDate);
        verificationTokenRepository.save(newToken);

        // Create email change request
        EmailChangeRequest emailChangeRequest = new EmailChangeRequest(newToken, newEmail);
        emailChangeRequestRepository.save(emailChangeRequest);

        // Send email with verification link
        emailSenderService.sendEmailChangeVerificationEmail(user, plainToken, newEmail);
    }
}
