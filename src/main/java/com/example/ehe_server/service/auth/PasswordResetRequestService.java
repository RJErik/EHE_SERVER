package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetRequestServiceInterface;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Value("${app.verification.token.expiry-hours}")
    private long tokenExpiryHours;

    public PasswordResetRequestService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailServiceInterface emailService,
            LoggingServiceInterface loggingService, AdminRepository adminRepository, UserContextService userContextService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> requestPasswordReset(String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate email format
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                loggingService.logAction("Password reset failed: Missing email");
                return response;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                response.put("success", false);
                response.put("message", "Please enter a valid email address");
                loggingService.logAction("Password reset failed: Invalid email format");
                return response;
            }

            // Hash the email and find user
            var userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                // For security reasons, don't reveal if email exists or not
                // But we still log it internally
                response.put("success", true);
                response.put("message", "If your email is registered, you will receive password reset instructions shortly.");
                response.put("showResendButton", true);
                loggingService.logAction("Password reset request for non-existent email: " + email);
                return response;
            }

            User user = userOpt.get();
            String userIdStr = String.valueOf(user.getUserId());
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Set audit context
            String role = "USER"; // All authenticated users have USER role

            if (isAdmin) {
                role = "ADMIN"; // Add ADMIN role if user is in Admin table
            }
            userContextService.setUser(userIdStr, role);

            // Check account status
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE &&
                    user.getAccountStatus() != User.AccountStatus.NONVERIFIED) {
                // For suspended accounts, still pretend all is well but log it
                response.put("success", true);
                response.put("message", "If your email is registered, you will receive password reset instructions shortly.");
                response.put("showResendButton", true);
                loggingService.logAction("Password reset requested for suspended account: " + email);
                return response;
            }

            // Rate limiting check
            LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
            long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                    user.getUserId(), VerificationToken.TokenType.PASSWORD_RESET, rateLimitThreshold);

            if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
                response.put("success", false);
                response.put("message", "Too many password reset requests. Please wait " +
                        RATE_LIMIT_MINUTES + " minutes before trying again.");
                response.put("showResendButton", true);
                loggingService.logAction("Password reset rate limit hit for: " + email);
                return response;
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
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours / 2);
            VerificationToken newToken = new VerificationToken(
                    user, token, VerificationToken.TokenType.PASSWORD_RESET, expiryDate);
            verificationTokenRepository.save(newToken);
            loggingService.logAction("Generated new password reset token");

            // Send email with reset link
            try {
                emailService.sendPasswordResetEmail(user, token, email);
                response.put("success", true);
                response.put("message", "If your email is registered, you will receive password reset instructions shortly.");
                response.put("showResendButton", true);
                return response;
            } catch (Exception e) {
                // Log failure but don't reveal to user
                loggingService.logError("Failed to send password reset email to " + email, e);
                // Throw to trigger transaction rollback
                throw e;
            }
        } catch (Exception e) {
            loggingService.logError("Error during password reset request: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again later.");
            return response;
        }
    }
}
