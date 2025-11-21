package com.example.ehe_server.service.email;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.AccountAlreadyActiveException;
import com.example.ehe_server.exception.custom.AccountSuspendedException;
import com.example.ehe_server.exception.custom.EmailSendFailureException;
import com.example.ehe_server.exception.custom.VerificationRateLimitExceededException;
import com.example.ehe_server.properties.EmailProperties;
import com.example.ehe_server.properties.FrontendProperties;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmailService implements EmailServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 5;

    private final JavaMailSender javaMailSender;
    private final LoggingServiceInterface loggingService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;
    private final FrontendProperties frontendProperties;
    private final VerificationTokenProperties verificationTokenProperties;
    private final EmailProperties emailProperties;

    public EmailService(JavaMailSender javaMailSender,
                        LoggingServiceInterface loggingService,
                        VerificationTokenRepository verificationTokenRepository,
                        AdminRepository adminRepository,
                        UserContextService userContextService,
                        FrontendProperties frontendProperties,
                        VerificationTokenProperties verificationTokenProperties,
                        EmailProperties emailProperties) {
        this.javaMailSender = javaMailSender;
        this.loggingService = loggingService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.frontendProperties = frontendProperties;
        this.verificationTokenProperties = verificationTokenProperties;
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendVerificationEmail(User user, String token, String recipientEmail) {
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        String subject = "Verify your Email for Event Horizon Exchange";
        String verificationUrl = frontendProperties.getFrontEndUrl() + "/verify-registration?token=" + token; // Adjust path if needed
        String text = "Dear " + user.getUserName() + ",\n\n"
                + "Thank you for registering. Please click the link below to verify your email address:\n"
                + verificationUrl + "\n\n"
                + "If you did not register, please ignore this email.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";
        sendSimpleMessage(recipientEmail, subject, text);
    }

    @Override
    @Async // Optional
    public void sendSimpleMessage(String to, String subject, String text) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailProperties.getUsername());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
        // Avoid logging here if called internally by other methods that already log
    }

    @Transactional
    public void resendVerificationEmail(User user, String email) {
        // Set audit context
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
            //SYSTEM SET HERE
            throw new AccountAlreadyActiveException(email).withActionLink("log in", "login");
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            //SYSTEM SET HERE
            throw new AccountSuspendedException(email);
        }

        // --- Rate Limiting Check ---
        LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                user.getUserId(), VerificationToken.TokenType.REGISTRATION, rateLimitThreshold);

        if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
            //SYSTEM SET HERE
            throw new VerificationRateLimitExceededException(RATE_LIMIT_MINUTES);
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
            //SYSTEM SET HERE
            loggingService.logAction("Invalidated " + tokensToUpdate.size() + " previous active verification token(s) for: " + email);
        }

        // --- Generate and Save New Token ---
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());
        VerificationToken newToken = new VerificationToken(user, token, VerificationToken.TokenType.REGISTRATION, expiryDate);
        verificationTokenRepository.save(newToken);
        //SYSTEM SET HERE
        loggingService.logAction("Generated new verification token for: " + email);

        // --- Send New Email ---
        try {
            // Use the original email provided in the request for sending
            sendVerificationEmail(user, token, email);
            // Email service should log success/failure of sending internally
        } catch (Exception e) {
            //SYSTEM SET HERE
            throw new EmailSendFailureException(email, e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(User user, String token, String recipientEmail) {
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        String subject = "Password Reset for Event Horizon Exchange";
        String resetUrl = frontendProperties.getFrontEndUrl() + "/reset-password?token=" + token;
        String text = "Dear " + user.getUserName() + ",\n\n"
                + "We received a request to reset your password. Please click the link below to reset your password:\n"
                + resetUrl + "\n\n"
                + "This link will expire in " + (verificationTokenProperties.getTokenExpiryHours() / 2) + " hours.\n\n"
                + "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";
        sendSimpleMessage(recipientEmail, subject, text);

        //SYSTEM SET HERE
        loggingService.logAction("Password reset email sent to: " + recipientEmail);
    }

    @Override
    public void sendEmailChangeVerificationEmail(User user, String token, String newEmail) {
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Create roles list based on user status
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        String subject = "Verify Email Change for Event Horizon Exchange";
        String verificationUrl = frontendProperties.getFrontEndUrl() + "/verify-email-change?token=" + token;
        String text = "Dear " + user.getUserName() + ",\n\n"
                + "We received a request to change your email address to this one. "
                + "Please click the link below to verify this email address and complete the change:\n"
                + verificationUrl + "\n\n"
                + "This link will expire in " + verificationTokenProperties.getTokenExpiryHours() + " hours.\n\n"
                + "If you did not request this change, please ignore this email or contact support if you have concerns.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";
        sendSimpleMessage(newEmail, subject, text);

        //SYSTEM SET HERE
        loggingService.logAction("Email change verification email sent to: " + newEmail);
    }


}