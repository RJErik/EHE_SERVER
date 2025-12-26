package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.properties.VerificationTokenProperties;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.auth.RegistrationVerificationResendServiceInterface;
import com.example.ehe_server.service.intf.email.EmailSenderServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class RegistrationVerificationResendService implements RegistrationVerificationResendServiceInterface {

    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 5;

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSenderServiceInterface emailSenderService;
    private final VerificationTokenProperties verificationTokenProperties;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final UserContextServiceInterface userContextService;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int EMAIL_MAX_LENGTH = 255;

    public RegistrationVerificationResendService(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            EmailSenderServiceInterface emailSenderService,
            VerificationTokenProperties verificationTokenProperties,
            AdminRepository adminRepository,
            UserContextServiceInterface userContextService,
            PasswordEncoder passwordEncoder) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.verificationTokenProperties = verificationTokenProperties;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(messageKey = "log.message.auth.resendRegistrationVerification")
    @Override
    public void resendVerificationEmail(String email) {

        // Input validation checks
        if (email == null || email.trim().isEmpty()) {
            throw new MissingEmailException();
        }

        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailFormatException(email);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserEmailNotFoundException(email);
        }

        User user = userOpt.get();

        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(userIdStr, role);

        // Account Status Checks
        if (user.getAccountStatus() == User.AccountStatus.ACTIVE) {
            throw new AccountAlreadyActiveException(email).withActionLink("log in", "login");
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new AccountSuspendedException(email);
        }

        // Rate Limiting Check
        LocalDateTime rateLimitThreshold = LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);
        long recentTokenCount = verificationTokenRepository.countByUser_UserIdAndTokenTypeAndIssueDateAfter(
                user.getUserId(), VerificationToken.TokenType.REGISTRATION, rateLimitThreshold);

        if (recentTokenCount >= RATE_LIMIT_MAX_REQUESTS) {
            throw new VerificationRateLimitExceededException(RATE_LIMIT_MINUTES);
        }

        // Invalidate existing active tokens
        List<VerificationToken> activeTokens = verificationTokenRepository.findByUser_UserIdAndTokenTypeAndStatus(
                user.getUserId(), VerificationToken.TokenType.REGISTRATION, VerificationToken.TokenStatus.ACTIVE);

        activeTokens.forEach(token -> token.setStatus(VerificationToken.TokenStatus.INVALIDATED));

        // Generate and Save New Token
        String plainToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(plainToken);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(verificationTokenProperties.getTokenExpiryHours());

        VerificationToken newToken = new VerificationToken(
                user, hashedToken, VerificationToken.TokenType.REGISTRATION, expiryDate);

        verificationTokenRepository.save(newToken);

        // Email notification
        try {
            emailSenderService.sendRegistrationVerificationEmail(user, plainToken, email);
        } catch (EmailSendFailureException e) {
            // This runtime exception triggers the rollback of the User creation, make sure to forward it with resend button
            throw new EmailSendFailureException(email, e.getMessage()).withResendButton();
        }
    }
}