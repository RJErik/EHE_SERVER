package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.EmailChangeRequestRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.user.EmailChangeRequestServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmailChangeRequestService implements EmailChangeRequestServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 3;
    private static final int RATE_LIMIT_MINUTES = 30;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final EmailServiceInterface emailService;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;

    @Value("${app.verification.token.expiry-hours}")
    private long tokenExpiryHours;

    public EmailChangeRequestService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            EmailServiceInterface emailService,
            LoggingServiceInterface loggingService, UserContextService userContextService, AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.emailService = emailService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> requestEmailChange(Long userId, String newEmail) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate email format
            if (newEmail == null || newEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                loggingService.logAction("Email change failed: Missing email");
                return response;
            }

            if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
                response.put("success", false);
                response.put("message", "Please enter a valid email address");
                loggingService.logAction("Email change failed: Invalid email format");
                return response;
            }

            // Find the user
            Optional<User> userOpt = userRepository.findById(userId.intValue());

            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                loggingService.logAction("Email change failed: User not found");
                return response;
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
                response.put("success", false);
                response.put("message", "Your account is not active");
                loggingService.logAction("Email change failed: Account not active, status=" + user.getAccountStatus());
                return response;
            }

            // Check if the new email is the same as the current one
            if (user.getEmail().equals(newEmail)) {
                response.put("success", false);
                response.put("message", "The new email is the same as your current email");
                loggingService.logAction("Email change failed: New email is the same as current");
                return response;
            }

            // Check if the new email is already in use by another user
            Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getUserId().equals(user.getUserId())) {
                response.put("success", false);
                response.put("message", "This email is already in use by another account");
                loggingService.logAction("Email change failed: Email already in use");
                return response;
            }

            // Rate limiting check
            LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
            long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                    user.getUserId(), VerificationToken.TokenType.EMAIL_CHANGE, rateLimitThreshold);

            if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
                response.put("success", false);
                response.put("message", "Too many email change requests. Please wait " +
                        RATE_LIMIT_MINUTES + " minutes before trying again.");
                loggingService.logAction("Email change rate limit hit");
                response.put("showResendButton", true);
                return response;
            }

            // Invalidate existing active tokens
            List<VerificationToken> oldTokens = verificationTokenRepository.findByUser_UserIdAndTokenType(
                    user.getUserId(), VerificationToken.TokenType.EMAIL_CHANGE);

            List<VerificationToken> tokensToUpdate = oldTokens.stream()
                    .filter(token -> token.getStatus() == VerificationToken.TokenStatus.ACTIVE)
                    .peek(token -> token.setStatus(VerificationToken.TokenStatus.INVALIDATED))
                    .collect(Collectors.toList());

            if (!tokensToUpdate.isEmpty()) {
                verificationTokenRepository.saveAll(tokensToUpdate);
                loggingService.logAction("Invalidated " + tokensToUpdate.size() + " previous active email change token(s)");
            }

            // Create new token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
            VerificationToken newToken = new VerificationToken(
                    user, token, VerificationToken.TokenType.EMAIL_CHANGE, expiryDate);
            verificationTokenRepository.save(newToken);

            // Create email change request
            EmailChangeRequest emailChangeRequest = new EmailChangeRequest(newToken, newEmail);
            emailChangeRequestRepository.save(emailChangeRequest);

            loggingService.logAction("Generated new email change token and request");

            // Send email with verification link
            try {
                emailService.sendEmailChangeVerificationEmail(user, token, newEmail);
                response.put("success", true);
                response.put("message", "A verification email has been sent to your new email address. " +
                        "Please check your inbox and click the verification link to complete the email change.");
                response.put("showResendButton", true);
                return response;
            } catch (Exception e) {
                // Log failure
                loggingService.logError("Failed to send email change verification email", e);
                // Throw to trigger transaction rollback
                throw e;
            }
        } catch (Exception e) {
            loggingService.logError("Error during email change request: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again later.");
            return response;
        }
    }
}
