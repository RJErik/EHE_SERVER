package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.PasswordResetTokenValidationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PasswordResetTokenValidationService implements PasswordResetTokenValidationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;

    public PasswordResetTokenValidationService(
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            UserContextService userContextService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @LogMessage(
            messageKey = "log.message.auth.passwordResetTokenValidation",
            params = {"#token"}
    )
    @Override
    public void validatePasswordResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new MissingTokenException();
        }

        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new InvalidPasswordResetTokenException(token);
        }

        VerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();

        if (user == null) {
            throw new UserNotFoundForTokenException(token);
        }

        // Set audit context if user found
        // Update audit context
        String userIdStr = String.valueOf(user.getUserId());
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

        // Set audit context
        String role = "USER"; // All authenticated users have USER role

        if (isAdmin) {
            role = "ADMIN"; // Add ADMIN role if user is in Admin table
        }
        userContextService.setUser(userIdStr, role);

        // Check token type
        if (verificationToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.PASSWORD_RESET, verificationToken.getTokenType());
        }

        // Check expiration
        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            throw new ExpiredVerificationTokenException(token).withResendButton();
        }

        // Check token status
        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }
    }
}
