package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetServiceInterface;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PasswordResetService implements PasswordResetServiceInterface {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenValidationServiceInterface validationService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public PasswordResetService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenValidationServiceInterface validationService,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.validationService = validationService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate token first
            Map<String, Object> validationResult = validationService.validatePasswordResetToken(token);
            boolean isTokenValid = (boolean) validationResult.getOrDefault("success", false);

            if (!isTokenValid) {
                // Use the validation error message
                return validationResult;
            }

            // Validate password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                loggingService.logAction(null, auditContextService.getCurrentUser(),
                        "Password reset failed: Password is empty");
                return response;
            }

            if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters with at least one letter and one number");
                loggingService.logAction(null, auditContextService.getCurrentUser(),
                        "Password reset failed: Password does not meet requirements");
                return response;
            }

            // Get token and user
            Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

            // This should not happen as we validated the token, but check anyway
            if (tokenOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid reset token");
                loggingService.logAction(null, auditContextService.getCurrentUser(),
                        "Password reset failed: Token not found after validation");
                return response;
            }

            VerificationToken verificationToken = tokenOpt.get();
            User user = verificationToken.getUser();

            // Check if user is null
            if (user == null) {
                response.put("success", false);
                response.put("message", "User not found for this token. Try ");
                Map<String, String> actionLink = new HashMap<>();
                actionLink.put("text", "registering");
                actionLink.put("target", "register");
                response.put("actionLink", actionLink);
                loggingService.logAction(null, auditContextService.getCurrentUser(),
                        "Password reset failed: User not found for token");
                return response;
            }

            // Update audit context
            auditContextService.setCurrentUser(String.valueOf(user.getUserId()));

            // Update password
            String passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPasswordHash(passwordHash);

            // If account was NONVERIFIED, activate it
            if (user.getAccountStatus() == User.AccountStatus.NONVERIFIED) {
                user.setAccountStatus(User.AccountStatus.ACTIVE);
                loggingService.logAction(user.getUserId(), auditContextService.getCurrentUser(),
                        "User account activated during password reset");
            }

            // Mark token as used
            verificationToken.setStatus(VerificationToken.TokenStatus.USED);

            // Save changes
            userRepository.save(user);
            verificationTokenRepository.save(verificationToken);

            loggingService.logAction(user.getUserId(), auditContextService.getCurrentUser(),
                    "Password reset successful");

            response.put("success", true);
            response.put("message", "Your password has been reset successfully. You can now log in with your new password.");
            return response;

        } catch (Exception e) {
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error during password reset: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again later.");
            return response;
        }
    }
}
