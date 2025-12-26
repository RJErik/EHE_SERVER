package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.EmailChangeRequest;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.EmailChangeRequestRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.EmailChangeVerificationServiceInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(noRollbackFor = ExpiredVerificationTokenException.class)
public class EmailChangeVerificationService implements EmailChangeVerificationServiceInterface {

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public EmailChangeVerificationService(
            VerificationTokenRepository verificationTokenRepository,
            EmailChangeRequestRepository emailChangeRequestRepository,
            UserRepository userRepository,
            UserContextService userContextService,
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailChangeRequestRepository = emailChangeRequestRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @LogMessage(
            messageKey = "log.message.auth.emailChangeVerification",
            params = {"#token"}
    )
    @Override
    public void validateEmailChange(String token) {

        // Input validation checks
        if (token == null || token.trim().isEmpty()) {
            throw new MissingVerificationTokenException(); // New Check
        }

        // Token lookup
        VerificationToken verificationToken = findTokenByPlainValue(
                token
        );

        if (verificationToken == null) {
            throw new InvalidVerificationTokenException(token);
        }


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

    private VerificationToken findTokenByPlainValue(String plainToken) {
        List<VerificationToken> activeTokens = verificationTokenRepository.findByTokenTypeAndStatus(
                VerificationToken.TokenType.EMAIL_CHANGE,
                VerificationToken.TokenStatus.ACTIVE
        );

        return activeTokens.stream()
                .filter(token -> passwordEncoder.matches(plainToken, token.getTokenHash()))
                .findFirst()
                .orElse(null);
    }

}