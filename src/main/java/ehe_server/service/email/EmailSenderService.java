package ehe_server.service.email;

import ehe_server.entity.User;
import ehe_server.exception.custom.EmailSendFailureException;
import ehe_server.properties.EmailProperties;
import ehe_server.properties.FrontendProperties;
import ehe_server.properties.VerificationTokenProperties;
import ehe_server.service.intf.email.EmailSenderServiceInterface;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailSenderService implements EmailSenderServiceInterface {

    private final JavaMailSender javaMailSender;
    private final FrontendProperties frontendProperties;
    private final VerificationTokenProperties verificationTokenProperties;
    private final EmailProperties emailProperties;

    public EmailSenderService(JavaMailSender javaMailSender,
                              FrontendProperties frontendProperties,
                              VerificationTokenProperties verificationTokenProperties,
                              EmailProperties emailProperties) {
        this.javaMailSender = javaMailSender;
        this.frontendProperties = frontendProperties;
        this.verificationTokenProperties = verificationTokenProperties;
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendRegistrationVerificationEmail(User user, String token, String recipientEmail) {
        String subject = "Verify your Email for Event Horizon Exchange";
        String verificationUrl = frontendProperties.getUrl()
                + frontendProperties.getVerifyRegistrationPath()
                + "?token=" + token;

        String text = "Dear " + user.getUserName() + ",\n\n"
                + "Thank you for registering. Please click the link below to verify your email address:\n"
                + verificationUrl + "\n\n"
                + "If you did not register, please ignore this email.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";

        sendSimpleMessage(recipientEmail, subject, text);
    }

    @Override
    public void sendPasswordResetEmail(User user, String token, String recipientEmail) {
        String subject = "Password Reset for Event Horizon Exchange";
        String resetUrl = frontendProperties.getUrl()
                + frontendProperties.getResetPasswordPath()
                + "?token=" + token;

        String text = "Dear " + user.getUserName() + ",\n\n"
                + "We received a request to reset your password. Please click the link below to reset your password:\n"
                + resetUrl + "\n\n"
                + "This link will expire in " + (verificationTokenProperties.getTokenExpiryHours()) + " hours.\n\n"
                + "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";

        sendSimpleMessage(recipientEmail, subject, text);
    }

    @Override
    public void sendEmailChangeVerificationEmail(User user, String token, String newEmail) {
        String subject = "Verify Email Change for Event Horizon Exchange";
        String verificationUrl = frontendProperties.getUrl()
                + frontendProperties.getVerifyEmailChangePath()
                + "?token=" + token;

        String text = "Dear " + user.getUserName() + ",\n\n"
                + "We received a request to change your email address to this one. "
                + "Please click the link below to verify this email address and complete the change:\n"
                + verificationUrl + "\n\n"
                + "This link will expire in " + verificationTokenProperties.getTokenExpiryHours() + " hours.\n\n"
                + "If you did not request this change, please ignore this email or contact support if you have concerns.\n\n"
                + "Regards,\nThe Event Horizon Exchange Team";

        sendSimpleMessage(newEmail, subject, text);
    }

    /**
     * Internal helper to handle the actual network transmission and exception wrapping.
     */
    private void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getUsername());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
        } catch (MailException e) {
            throw new EmailSendFailureException(to, e.getMessage());
        }
    }
}