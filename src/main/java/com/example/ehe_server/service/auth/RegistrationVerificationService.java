package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.RegistrationVerificationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class RegistrationVerificationService implements RegistrationVerificationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final LoggingServiceInterface loggingService;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;

    public RegistrationVerificationService(UserRepository userRepository,
                                           VerificationTokenRepository verificationTokenRepository,
                                           LoggingServiceInterface loggingService, AdminRepository adminRepository, UserContextService userContextService) { // Inject EmailService
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.loggingService = loggingService;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyRegistrationToken(String token) {
        Map<String, Object> response = new HashMap<>();
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid verification token. The link may be incorrect or the token does not exist.");
            // Cannot reliably log user ID here as token is invalid
            loggingService.logAction("Verification attempt with non-existent token: " + token);
            return response;
        }

        VerificationToken verificationToken = tokenOpt.get();
        // Eagerly fetch user if needed, or assume lazy loading works within transaction
        User user = verificationToken.getUser();
        String userIdStr = user != null ? String.valueOf(user.getUserId()) : "unknown"; // Handle potential null user?

        // Set audit context if user found
        if (user != null) {
            // Update audit context
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Set audit context
            String role = "USER"; // All authenticated users have USER role

            if (isAdmin) {
                role = "ADMIN"; // Add ADMIN role if user is in Admin table
            }
            userContextService.setUser(userIdStr, role);
        }

        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            response.put("success", false);
            response.put("message", "Verification token has expired. Please request a new verification email.");
            response.put("showResendButton", true);

            loggingService.logAction("Verification attempt with expired token: " + token);

            return response;
        }

        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            response.put("success", false);
            response.put("message", "Token is not active. It may have already been used or invalidated.");
            loggingService.logAction("Verification attempt with non-active token (" + verificationToken.getStatus() + "): " + token);
            return response;
        }



        // Optional: Check token type if tokens are used for multiple purposes
        if (verificationToken.getTokenType() != VerificationToken.TokenType.REGISTRATION) {
            response.put("success", false);
            response.put("message", "Invalid token type provided. This link cannot be used for account verification.");
            loggingService.logAction("Verification attempt with wrong token type (" + verificationToken.getTokenType() + "): " + token);
            return response;
        }

        // --- If checks pass, verify the user ---
        if (user == null) { // Should not happen if FK constraint exists, but check anyway
            response.put("success", false);
            response.put("message", "Internal error: User associated with token not found.");
            loggingService.logError("Token " + verificationToken.getVerificationTokenId() + " has no associated user.", new IllegalStateException("User is null for valid token"));
            return response;
        }

        user.setAccountStatus(User.AccountStatus.ACTIVE);
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        userRepository.save(user);
        verificationTokenRepository.save(verificationToken); // Save updated token status

        loggingService.logAction("User account successfully verified with token: " + token);
        response.put("success", true);
        response.put("message", "Account verified successfully. You can now log in.");

        return response;
    }
}