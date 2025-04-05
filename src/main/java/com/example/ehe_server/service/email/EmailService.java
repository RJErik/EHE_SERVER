package com.example.ehe_server.service.email;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.email.EmailServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // Optional: for asynchronous email sending
import org.springframework.stereotype.Service;

@Service
public class EmailService implements EmailServiceInterface {

    private final JavaMailSender mailSender;
    private final LoggingServiceInterface loggingService;
    private final AuditContextService auditContextService;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Define a system user ID for audit logs triggered by email sending
    private static final String AUDIT_USER_EMAIL_SERVICE = "SYSTEM_EMAIL_SERVICE";

    public EmailService(JavaMailSender mailSender,
                        LoggingServiceInterface loggingService,
                        AuditContextService auditContextService) {
        this.mailSender = mailSender;
        this.loggingService = loggingService;
        this.auditContextService = auditContextService;
    }

    @Override
    @Async // Optional: Make email sending non-blocking
    public void sendVerificationEmail(User user, String token, String recipientEmail) {
        // Set audit context for this operation
        auditContextService.setCurrentUser(AUDIT_USER_EMAIL_SERVICE);
        auditContextService.setCurrentUserRole("SYSTEM"); // Assign a role if applicable

        String subject = "Verify your Email for Event Horizon Exchange";
        String verificationUrl = frontendUrl + "/verify?token=" + token; // Adjust path if needed
        String text = "Dear " + user.getUserName() + ",\n\n"
                + "Thank you for registering. Please click the link below to verify your email address:\n"
                + verificationUrl + "\n\n"
                + "If you did not register, please ignore this email.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";

        try {
            sendSimpleMessage(recipientEmail, subject, text);
            loggingService.logAction(user.getUserId(), AUDIT_USER_EMAIL_SERVICE, "Sent verification email to " + recipientEmail);
        } catch (MailException e) {
            loggingService.logError(user.getUserId(), AUDIT_USER_EMAIL_SERVICE, "Failed to send verification email to " + recipientEmail, e);
            // Consider how to handle failures - retry? notify admin?
        } finally {
            // Optional: Reset context if needed, or rely on filter to reset it
            // auditContextService.setCurrentUser("unknown");
        }
    }

    /*
    @Override
    @Async // Optional
    public void sendPasswordResetEmail(User user, String token, String recipientEmail) {
        // Implement later
    }
    */

    @Override
    @Async // Optional
    public void sendSimpleMessage(String to, String subject, String text) {
        // No specific user context needed for generic sender, or use AUDIT_USER_EMAIL_SERVICE
        auditContextService.setCurrentUser(AUDIT_USER_EMAIL_SERVICE);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            // Avoid logging here if called internally by other methods that already log
        } catch (MailException e) {
            loggingService.logError(null, AUDIT_USER_EMAIL_SERVICE, "Failed to send simple email to " + to, e);
            throw e; // Re-throw for the caller to handle if necessary
        }
    }
}