package ehe_server.service.auth;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.User;
import ehe_server.entity.VerificationToken;
import ehe_server.exception.custom.*;
import ehe_server.repository.AdminRepository;
import ehe_server.repository.VerificationTokenRepository;
import ehe_server.service.audit.UserContextService;
import ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import ehe_server.service.intf.token.TokenHashServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = {ExpiredVerificationTokenException.class})
public class PasswordResetTokenValidationService implements PasswordResetTokenValidationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;
    private final TokenHashServiceInterface tokenHashService;

    public PasswordResetTokenValidationService(
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            UserContextService userContextService,
            TokenHashServiceInterface tokenHashService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.tokenHashService = tokenHashService;
    }

    @LogMessage(
            messageKey = "log.message.auth.passwordResetTokenValidation",
            params = {"#token"}
    )
    @Override
    public void validatePasswordResetToken(String token) {

        // Database integrity checks
        String hashedToken = tokenHashService.hashToken(token);

        VerificationToken verificationToken = verificationTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidVerificationTokenException(token));

        User user = verificationToken.getUser();

        if (user == null) {
            throw new UserNotFoundForTokenException(token);
        }

        // Audit context setup
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Token logical verification
        if (verificationToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.PASSWORD_RESET, verificationToken.getTokenType());
        }

        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }

        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            throw new ExpiredVerificationTokenException(token).withResendButton();
        }
    }
}