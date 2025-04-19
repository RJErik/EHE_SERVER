package com.example.ehe_server.service.auth;

import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.repository.EmailChangeRequestRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.auth.EmailChangeVerificationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailChangeVerificationService implements EmailChangeVerificationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    public EmailChangeVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService,
            AuditContextService auditContextService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyEmailChange(String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Find the token
            Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

            if (tokenOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid verification token");
                loggingService.logAction(null, auditContextService.getCurrentUser(),
                        "Email change verification failed: Invalid token");
                return response;
            }

            VerificationToken verificationToken = tokenOpt.get();
            User user = verificationToken.getUser();
            String userIdStr = String.valueOf(user.getUserId());

            // Set audit context
            auditContextService.setCurrentUser(userIdStr);

            // Check token status
            if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
                response.put("success", false);
                response.put("message", "This verification link has already been used or invalidated");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Token not active, status=" + verificationToken.getStatus());
                return response;
            }

            // Check token type
            if (verificationToken.getTokenType() != VerificationToken.TokenType.EMAIL_CHANGE) {
                response.put("success", false);
                response.put("message", "Invalid token type");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Wrong token type");
                return response;
            }

            // Check if token is expired
            if (verificationToken.isExpired()) {
                verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
                verificationTokenRepository.save(verificationToken);

                response.put("success", false);
                response.put("message", "This verification link has expired. Please request a new email change.");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Token expired");
                return response;
            }

            // Check user status
            if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
                response.put("success", false);
                response.put("message", "Your account is not active");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Account not active, status=" + user.getAccountStatus());
                return response;
            }

            // Find email change request
            Optional<EmailChangeRequest> requestOpt = emailChangeRequestRepository.findByVerificationToken(verificationToken);

            if (requestOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Email change request not found");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Request not found for token");
                return response;
            }

            EmailChangeRequest emailChangeRequest = requestOpt.get();
            String newEmail = emailChangeRequest.getNewEmail();

            // Check if the new email is already in use by another user
            Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getUserId().equals(user.getUserId())) {
                response.put("success", false);
                response.put("message", "This email is already in use by another account");
                loggingService.logAction(user.getUserId(), userIdStr,
                        "Email change verification failed: Email already in use");
                return response;
            }

            // Update user's email
            user.setEmail(newEmail);
            userRepository.save(user);

            // Mark token as used
            verificationToken.setStatus(VerificationToken.TokenStatus.USED);
            verificationTokenRepository.save(verificationToken);

            loggingService.logAction(user.getUserId(), userIdStr,
                    "Email successfully changed to: " + newEmail);

            response.put("success", true);
            response.put("message", "Your email has been successfully updated to " + newEmail);
            return response;

        } catch (Exception e) {
            loggingService.logError(null, auditContextService.getCurrentUser(),
                    "Error during email change verification: " + e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again later.");
            return response;
        }
    }
}
