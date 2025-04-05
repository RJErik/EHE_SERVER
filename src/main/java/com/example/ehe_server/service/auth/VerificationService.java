package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.auth.HashingServiceInterface;
import com.example.ehe_server.service.intf.auth.VerificationServiceInterface;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VerificationService implements VerificationServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 5;

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final HashingServiceInterface hashingService;
    private final EmailServiceInterface emailService;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    @Value("${app.verification.token.expiry-hours}")
    private long tokenExpiryHours;

    public VerificationService(UserRepository userRepository,
                               VerificationTokenRepository verificationTokenRepository,
                               HashingServiceInterface hashingService,
                               EmailServiceInterface emailService,
                               LoggingServiceInterface loggingService,
                               AuditContextService auditContextService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.hashingService = hashingService;
        this.emailService = emailService;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> resendVerification(String email) {
        Map<String, Object> response = new HashMap<>();
        String emailHash = hashingService.hashEmail(email);
        Optional<User> userOpt = userRepository.findByEmailHash(emailHash);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("status", "ERROR_USER_NOT_FOUND");
            response.put("message", "User not found with the provided email.");
            response.put("actions", Collections.emptyList());
            return response;
        }

        User user = userOpt.get();
        String userIdStr = String.valueOf(user.getUserId());

        // Set audit context
        auditContextService.setCurrentUser(userIdStr);
        auditContextService.setCurrentUserRole("USER"); // Assuming role context

        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
            response.put("success", false);
            response.put("status", "ERROR_ALREADY_VERIFIED");
            response.put("message", "This account is already verified.");
            response.put("details", "You can log in directly.");
            response.put("actions", Collections.emptyList());
            loggingService.logAction(user.getUserId(), userIdStr, "Attempted resend for already active account: " + email);
            return response;
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            response.put("success", false);
            response.put("status", "ERROR_ACCOUNT_SUSPENDED");
            response.put("message", "This account is suspended.");
            response.put("details", "Contact support for assistance.");
            response.put("actions", Collections.emptyList());
            loggingService.logAction(user.getUserId(), userIdStr, "Attempted resend for suspended account: " + email);
            return response;
        }

        // --- Rate Limiting Check ---
        LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                user.getUserId(), VerificationToken.TokenType.REGISTRATION, rateLimitThreshold);

        if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
            response.put("success", false);
            response.put("status", "RATE_LIMITED");
            response.put("message", "Too many verification requests.");
            response.put("details", "Please wait " + RATE_LIMIT_MINUTES + " minutes before trying again.");
            // Still provide the action, but frontend uses message/status
            response.put("actions", List.of(createResendAction(email)));
            loggingService.logAction(user.getUserId(), userIdStr, "Verification resend rate limit hit for: " + email);
            return response;
        }

        // --- Invalidate Old ACTIVE Tokens ---
        List<VerificationToken> oldTokens = verificationTokenRepository.findByUser_UserIdAndTokenType(
                user.getUserId(), VerificationToken.TokenType.REGISTRATION);

        List<VerificationToken> tokensToUpdate = oldTokens.stream()
                .filter(token -> token.getStatus() == VerificationToken.TokenStatus.ACTIVE)
                .peek(token -> token.setStatus(VerificationToken.TokenStatus.INVALIDATED))
                .collect(Collectors.toList());

        if (!tokensToUpdate.isEmpty()) {
            verificationTokenRepository.saveAll(tokensToUpdate);
            loggingService.logAction(user.getUserId(), userIdStr, "Invalidated " + tokensToUpdate.size() + " previous active verification token(s) for: " + email);
        }

        // --- Generate and Save New Token ---
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
        VerificationToken newToken = new VerificationToken(user, token, VerificationToken.TokenType.REGISTRATION, expiryDate);
        verificationTokenRepository.save(newToken);
        loggingService.logAction(user.getUserId(), userIdStr, "Generated new verification token for: " + email);

        // --- Send New Email ---
        try {
            // Use the original email provided in the request for sending
            emailService.sendVerificationEmail(user, token, email);
            // Email service should log success/failure of sending internally
        } catch (Exception e) {
            // Log critical failure if exception bubbles up
            loggingService.logError(user.getUserId(), userIdStr, "Critical failure attempting to send verification email for resend to " + email, e);
            // Rollback should be handled by @Transactional. Return an error response.
            response.put("success", false);
            response.put("status", "ERROR_EMAIL_FAILURE");
            response.put("message", "Could not send verification email due to an internal error.");
            response.put("details", "Please try again later or contact support.");
            // Provide action to try again
            response.put("actions", List.of(createResendAction(email)));
            // Don't throw here unless you want specific higher-level handling
            return response; // Return error, transaction will likely rollback anyway if needed
        }

        // --- Return Success ---
        response.put("success", true);
        response.put("status", "RESEND_SUCCESS");
        response.put("message", "Verification email resent successfully to " + email + ".");
        response.put("details", "Please check your inbox (and spam folder).");
        response.put("actions", List.of(createResendAction(email, "Resend Verification Email Again")));
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyToken(String token) {
        Map<String, Object> response = new HashMap<>();
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            response.put("success", false);
            response.put("status", "ERROR_INVALID_TOKEN");
            response.put("message", "Invalid verification token.");
            response.put("details", "The link may be incorrect or the token does not exist.");
            response.put("actions", Collections.emptyList());
            // Cannot reliably log user ID here as token is invalid
            loggingService.logAction(null, "unknown", "Verification attempt with non-existent token: " + token);
            return response;
        }

        VerificationToken verificationToken = tokenOpt.get();
        // Eagerly fetch user if needed, or assume lazy loading works within transaction
        User user = verificationToken.getUser();
        String userIdStr = user != null ? String.valueOf(user.getUserId()) : "unknown_user"; // Handle potential null user?

        // Set audit context if user found
        if(user != null) {
            auditContextService.setCurrentUser(userIdStr);
            auditContextService.setCurrentUserRole("USER"); // Or appropriate role context for verification
        }

        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            response.put("success", false);
            response.put("status", "ERROR_TOKEN_NOT_ACTIVE");
            response.put("message", "Token is not active.");
            response.put("details", "It may have already been used, expired, or invalidated.");
            response.put("actions", Collections.emptyList());
            loggingService.logAction(user != null ? user.getUserId() : null, userIdStr, "Verification attempt with non-active token ("+ verificationToken.getStatus() +"): " + token);
            return response;
        }

        if (verificationToken.isExpired()) {
            // Update status to EXPIRED before returning error
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            verificationTokenRepository.save(verificationToken);
            response.put("success", false);
            response.put("status", "ERROR_TOKEN_EXPIRED");
            response.put("message", "Verification token has expired.");
            response.put("details", "Please request a new verification email.");
            // We need the user's email to provide a resend action here.
            // This requires fetching the email (or storing it with token - not ideal)
            // Or maybe frontend redirects to a page where user re-enters email for resend.
            // For now, no action provided.
            response.put("actions", Collections.emptyList()); // Or potentially a resend action if email is available
            loggingService.logAction(user != null ? user.getUserId() : null, userIdStr, "Verification attempt with expired token: " + token);
            return response;
        }

        // Optional: Check token type if tokens are used for multiple purposes
        if (verificationToken.getTokenType() != VerificationToken.TokenType.REGISTRATION) {
            response.put("success", false);
            response.put("status", "ERROR_WRONG_TOKEN_TYPE");
            response.put("message", "Invalid token type provided.");
            response.put("details", "This link cannot be used for account verification.");
            response.put("actions", Collections.emptyList());
            loggingService.logAction(user != null ? user.getUserId() : null, userIdStr, "Verification attempt with wrong token type ("+ verificationToken.getTokenType() +"): " + token);
            return response;
        }

        // --- If checks pass, verify the user ---
        if(user == null) { // Should not happen if FK constraint exists, but check anyway
            response.put("success", false);
            response.put("status", "ERROR_INTERNAL_USER_MISSING");
            response.put("message", "Internal error: User associated with token not found.");
            response.put("actions", Collections.emptyList());
            loggingService.logError(null, "system", "Token " + verificationToken.getVerificationTokenId() + " has no associated user.", new IllegalStateException("User is null for valid token"));
            return response;
        }

        user.setAccountStatus(User.AccountStatus.ACTIVE);
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        userRepository.save(user);
        verificationTokenRepository.save(verificationToken); // Save updated token status

        loggingService.logAction(user.getUserId(), userIdStr, "User account successfully verified with token: " + token);
        response.put("success", true);
        response.put("status", "VERIFICATION_SUCCESS");
        response.put("message", "Account verified successfully.");
        response.put("details", "You can now log in.");
        // Maybe add login action? e.g., { "type": "NAVIGATE", "target": "/login" }
        response.put("actions", Collections.emptyList());

        return response;
    }

    // Helper to create the action map consistently
    private Map<String, String> createResendAction(String email) {
        return createResendAction(email, "Resend Verification Email");
    }

    private Map<String, String> createResendAction(String email, String label) {
        Map<String, String> action = new HashMap<>();
        action.put("type", "RESEND_VERIFICATION");
        action.put("label", label);
        action.put("targetEmail", email);
        return action;
    }
}