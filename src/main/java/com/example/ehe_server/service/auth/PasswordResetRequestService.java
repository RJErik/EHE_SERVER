package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.InvalidEmailFormatException;
import com.example.ehe_server.exception.custom.MissingEmailException;
import com.example.ehe_server.exception.custom.PasswordResetRateLimitException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class PasswordResetRequestService implements PasswordResetRequestServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 5;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailServiceInterface emailService;
    private final LoggingServiceInterface loggingService;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;
    private final VerificationTokenProperties verificationTokenProperties;

    public PasswordResetRequestService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailServiceInterface emailService,
            LoggingServiceInterface loggingService,
            AdminRepository adminRepository,
            UserContextService userContextService,
            VerificationTokenProperties verificationTokenProperties) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.verificationTokenProperties = verificationTokenProperties;
    }

    @Override
    public void requestPasswordResetForUnauthenticatedUser(String email) {
        // Validate email format
        if (email == null || email.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }

        // Hash the email and find user
        var userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // For security reasons, don't reveal if email exists or not
            // But we still log it internally
            loggingService.logAction("Password reset request for non-existent email: " + email);
            return;
        }

        User user = userOpt.get();

        // Set audit context for unauthenticated users
        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(userIdStr, role);

        // Check account status for unauthenticated users
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE &&
                user.getAccountStatus() != User.AccountStatus.NONVERIFIED) {
            // For suspended accounts, still pretend all is well but log it
            loggingService.logAction("Password reset requested for suspended account: " + email);
            return;
        }

        processPasswordResetRequest(user, email);
    }

    @Override
    public void requestPasswordResetForAuthenticatedUser(Integer userId) {
        // Find user by ID (authenticated user context)
        var userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        User user = userOpt.get();
        String email = user.getEmail();

        // For authenticated users, the security filter already handles:
        // - User context setup
        // - Role assignment
        // - Account status validation
        // So we can go directly to processing the request

        processPasswordResetRequest(user, email);
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
        List<VerificationToken> oldTokens = verificationTokenRepository.findByUser_UserIdAndTokenType(
                user.getUserId(), VerificationToken.TokenType.PASSWORD_RESET);

        List<VerificationToken> tokensToUpdate = oldTokens.stream()
                .filter(token -> token.getStatus() == VerificationToken.TokenStatus.ACTIVE)
                .peek(token -> token.setStatus(VerificationToken.TokenStatus.INVALIDATED))
                .collect(Collectors.toList());

        if (!tokensToUpdate.isEmpty()) {
            verificationTokenRepository.saveAll(tokensToUpdate);
            loggingService.logAction("Invalidated " + tokensToUpdate.size() + " previous active password reset token(s)");
        }

        // Create new token - with shorter expiry for password resets
        String token = UUID.randomUUID().toString();
        // Use half the standard expiry time for password resets
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours() / 2);
        VerificationToken newToken = new VerificationToken(
                user, token, VerificationToken.TokenType.PASSWORD_RESET, expiryDate);
        verificationTokenRepository.save(newToken);
        loggingService.logAction("Generated new password reset token");

        // Send email with reset link
        try {
            emailService.sendPasswordResetEmail(user, token, email);
        } catch (Exception e) {
            // Log failure but don't reveal to user
            loggingService.logError("Failed to send password reset email to " + email, e);
            // Throw to trigger transaction rollback
            throw e;
        }
    }
}