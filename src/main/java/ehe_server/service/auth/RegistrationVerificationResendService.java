package ehe_server.service.auth;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.User;
import ehe_server.entity.VerificationToken;
import ehe_server.exception.custom.*;
import ehe_server.properties.VerificationTokenProperties;
import ehe_server.repository.AdminRepository;
import ehe_server.repository.UserRepository;
import ehe_server.repository.VerificationTokenRepository;
import ehe_server.service.intf.audit.UserContextServiceInterface;
import ehe_server.service.intf.auth.RegistrationVerificationResendServiceInterface;
import ehe_server.service.intf.email.EmailSenderServiceInterface;
import ehe_server.service.intf.token.TokenHashServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final TokenHashServiceInterface tokenHashService;

    public RegistrationVerificationResendService(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            EmailSenderServiceInterface emailSenderService,
            VerificationTokenProperties verificationTokenProperties,
            AdminRepository adminRepository,
            UserContextServiceInterface userContextService,
            TokenHashServiceInterface tokenHashService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.emailSenderService = emailSenderService;
        this.verificationTokenProperties = verificationTokenProperties;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(messageKey = "log.message.auth.resendRegistrationVerification")
    @Override
    public void resendVerificationEmail(String email) {

        // Input validation checks
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
        String hashedToken = tokenHashService.hashToken(plainToken);
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