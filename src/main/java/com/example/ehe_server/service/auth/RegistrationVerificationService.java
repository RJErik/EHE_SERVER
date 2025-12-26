package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.RegistrationVerificationServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RegistrationVerificationService implements RegistrationVerificationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            UserContextService userContextService,
            PasswordEncoder passwordEncoder) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(
            messageKey = "log.message.auth.registrationVerification",
            params = {"#token"}
    )
    @Override
    public void verifyRegistrationToken(String token) {

        // Input validation checks
        if (token == null || token.trim().isEmpty()) {
            throw new MissingVerificationTokenException();
        }

        // Database integrity checks
        VerificationToken verificationToken = findTokenByPlainValue(
                token
        );

        if (verificationToken == null) {
            throw new InvalidVerificationTokenException(token);
        }

        User user = verificationToken.getUser();

        if (user == null) {
            throw new UserNotFoundForTokenException(token);
        }

        // Audit context setup
        boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
        String role = isAdmin ? "ADMIN" : "USER";
        userContextService.setUser(String.valueOf(user.getUserId()), role);

        // Token logical verification
        if (verificationToken.getTokenType() != VerificationToken.TokenType.REGISTRATION) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.REGISTRATION, verificationToken.getTokenType());
        }

        // Check Status
        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }

        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            throw new ExpiredVerificationTokenException(token).withResendButton();
        }

        // State updates
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);
    }

    private VerificationToken findTokenByPlainValue(String plainToken) {
        // Get all active tokens of the specified type
        List<VerificationToken> activeTokens = verificationTokenRepository.findByTokenTypeAndStatus(
                VerificationToken.TokenType.REGISTRATION,
                VerificationToken.TokenStatus.ACTIVE
        );

        // Find the token that matches the plain value
        return activeTokens.stream()
                .filter(token -> passwordEncoder.matches(plainToken, token.getTokenHash()))
                .findFirst()
                .orElse(null);
    }
}