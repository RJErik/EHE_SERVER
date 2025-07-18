package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetTokenValidationService implements PasswordResetTokenValidationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final LoggingServiceInterface loggingService;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;

    public PasswordResetTokenValidationService(
            VerificationTokenRepository verificationTokenRepository,
            LoggingServiceInterface loggingService, AdminRepository adminRepository, UserContextService userContextService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional(readOnly = true) // Read-only since we're only validating
    public Map<String, Object> validatePasswordResetToken(String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (token == null || token.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Token is required");
                loggingService.logAction("Password reset token validation failed: Token is empty");
                return response;
            }

            Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

            if (tokenOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid password reset link. The link may be incorrect or the token does not exist.");
                loggingService.logAction("Password reset validation failed: Token not found: " + token);
                return response;
            }

            VerificationToken verificationToken = tokenOpt.get();
            User user = verificationToken.getUser();

            // Set audit context if user found
            if (user != null) {
                // Update audit context
                String userIdStr = String.valueOf(user.getUserId());
                boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

                // Set audit context
                String role = "USER"; // All authenticated users have USER role

                if (isAdmin) {
                    role = "ADMIN"; // Add ADMIN role if user is in Admin table
                }
                userContextService.setUser(userIdStr, role);
            }

            // Check token type
            if (verificationToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
                response.put("success", false);
                response.put("message", "Invalid token type. This link cannot be used for password reset.");
                loggingService.logAction("Password reset validation failed: Wrong token type: " + verificationToken.getTokenType());
                return response;
            }

            // Check expiration
            if (verificationToken.isExpired()) {
                verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
                response.put("success", false);
                response.put("message", "This password reset link has expired. Please request a new one.");
                response.put("showResendButton", true);
                loggingService.logAction("Password reset validation failed: Token expired");
                return response;
            }

            // Check token status
            if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
                response.put("success", false);
                response.put("message", "This password reset link is no longer valid. It may have already been used or expired.");
                loggingService.logAction("Password reset validation failed: Token not active: " + verificationToken.getStatus());
                return response;
            }

            // All checks passed
            response.put("success", true);
            response.put("message", "Token is valid");
            loggingService.logAction("Password reset token validated successfully");

            return response;
        } catch (Exception e) {
            loggingService.logError("Error validating password reset token: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again later.");
            return response;
        }
    }
}
