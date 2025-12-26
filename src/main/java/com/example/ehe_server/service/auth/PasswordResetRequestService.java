package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.email.EmailSenderServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class PasswordResetRequestService implements PasswordResetRequestServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 5;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSenderServiceInterface emailSenderService;
    private final LoggingServiceInterface loggingService;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;
    private final VerificationTokenProperties verificationTokenProperties;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetRequestService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailSenderServiceInterface emailSenderService,
            LoggingServiceInterface loggingService,
            AdminRepository adminRepository,
            UserContextService userContextService,
            VerificationTokenProperties verificationTokenProperties,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailSenderService = emailSenderService;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.verificationTokenProperties = verificationTokenProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(
            messageKey = "log.message.auth.passwordResetRequestUnauthenticated",
            params = {"#email"}
    )
    @Override
    public void requestPasswordResetForUnauthenticatedUser(String email) {

        // Input validation checks
        if (email == null || email.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }

        // Database integrity checks
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            loggingService.logAction("Password reset request for non-existent email: " + email);
            return;
        }

        User user = userOpt.get();

        // Security & Audit context setup
        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(userIdStr, role);

        // Account status verification
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE &&
                user.getAccountStatus() != User.AccountStatus.NONVERIFIED) {
            loggingService.logAction("Password reset requested for suspended account: " + email);
            return;
        }

        processPasswordResetRequest(user, email);
    }

    @LogMessage(messageKey = "log.message.auth.passwordResetRequestAuthenticated")
    @Override
    public void requestPasswordResetForAuthenticatedUser(Integer userId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Database integrity checks
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        User user = userOpt.get();
        processPasswordResetRequest(user, user.getEmail());
    }

    private void processPasswordResetRequest(User user, String email) {

        // Rate limiting check
        LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        int recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                user.getUserId(), VerificationToken.TokenType.PASSWORD_RESET, rateLimitThreshold);

        if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
            throw new PasswordResetRateLimitException(email, RATE_LIMIT_MAX_REQUESTS, RATE_LIMIT_MINUTES).withResendButton();
        }

        // Invalidate existing active tokens
        List<VerificationToken> activeTokens = verificationTokenRepository.findByUser_UserIdAndTokenTypeAndStatus(
                user.getUserId(), VerificationToken.TokenType.PASSWORD_RESET, VerificationToken.TokenStatus.ACTIVE);

        activeTokens.forEach(token -> token.setStatus(VerificationToken.TokenStatus.INVALIDATED));

        // Token generation
        String plainToken = UUID.randomUUID().toString();  // Plain token to send via email
        String hashedToken = passwordEncoder.encode(plainToken);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());

        VerificationToken newToken = new VerificationToken(
                user, hashedToken, VerificationToken.TokenType.PASSWORD_RESET, expiryDate);
        verificationTokenRepository.save(newToken);

        // Email notification
        emailSenderService.sendPasswordResetEmail(user, plainToken, email);
    }
}