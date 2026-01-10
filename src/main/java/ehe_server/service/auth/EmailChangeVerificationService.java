package ehe_server.service.auth;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.EmailChangeRequest;
import ehe_server.entity.User;
import ehe_server.entity.VerificationToken;
import ehe_server.exception.custom.*;
import ehe_server.repository.AdminRepository;
import ehe_server.repository.EmailChangeRequestRepository;
import ehe_server.repository.UserRepository;
import ehe_server.repository.VerificationTokenRepository;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.auth.EmailChangeVerificationServiceInterface;
import ehe_server.service.intf.token.TokenHashServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(noRollbackFor = {ExpiredVerificationTokenException.class, EmailAlreadyInUseException.class})
public class EmailChangeVerificationService implements EmailChangeVerificationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;
    private final TokenHashServiceInterface tokenHashService;

    public EmailChangeVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            UserRepository userRepository,
            UserContextService userContextService,
            AdminRepository adminRepository,
            TokenHashServiceInterface tokenHashService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(
            messageKey = "log.message.auth.emailChangeVerification",
            params = {"#token"}
    )
    @Override
    public void validateEmailChange(String token) {

        // Token lookup
        String hashedToken = tokenHashService.hashToken(token);

        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidVerificationTokenException(token));


        // Audit Context Setup
        User user = verificationToken.getUser();
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Token Logic Validation
        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }

        if (verificationToken.getTokenType() != VerificationToken.TokenType.EMAIL_CHANGE) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.EMAIL_CHANGE, verificationToken.getTokenType());
        }

        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            verificationTokenRepository.save(verificationToken);
            throw new ExpiredVerificationTokenException(token);
        }

        // User Account Validation
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new InactiveAccountException(String.valueOf(user.getUserId()), user.getAccountStatus().toString());
        }

        // Request Logic Validation
        EmailChangeRequest emailChangeRequest = emailChangeRequestRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new EmailChangeRequestNotFoundException(token));

        String newEmail = emailChangeRequest.getNewEmail();

        // Check for email collision
        Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
        if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getUserId().equals(user.getUserId())) {
            verificationToken.setStatus(VerificationToken.TokenStatus.INVALIDATED);
            throw new EmailAlreadyInUseException(newEmail);
        }

        // Invalidate existing active tokens
        List<VerificationToken> activeTokens = verificationTokenRepository.findByUser_UserIdAndTokenTypeAndStatus(
                user.getUserId(), VerificationToken.TokenType.EMAIL_CHANGE, VerificationToken.TokenStatus.ACTIVE);

        activeTokens.forEach(activeToken -> activeToken.setStatus(VerificationToken.TokenStatus.INVALIDATED));

        // Execution and Persistence
        user.setEmail(newEmail);
        userRepository.save(user);

        verificationToken.setStatus(VerificationToken.TokenStatus.USED);
        verificationTokenRepository.save(verificationToken);
    }
}