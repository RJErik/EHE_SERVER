package com.example.ehe_server.service.auth;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.VerificationToken;
import com.example.ehe_server.exception.custom.ExpiredVerificationTokenException;
import com.example.ehe_server.exception.custom.InactiveTokenException;
import com.example.ehe_server.exception.custom.InvalidVerificationTokenException;
import com.example.ehe_server.exception.custom.TokenTypeMismatchException;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.VerificationTokenRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.RegistrationVerificationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class RegistrationVerificationService implements RegistrationVerificationServiceInterface {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminRepository adminRepository;
    private final UserContextService userContextService;

    public RegistrationVerificationService(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            AdminRepository adminRepository,
            UserContextService userContextService) { // Inject EmailService
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.adminRepository = adminRepository;
        this.userContextService = userContextService;
    }

    @LogMessage(
            messageKey = "log.message.auth.registrationVerification",
            params = {"#token"}
    )
    @Override
    public void verifyRegistrationToken(String token) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new InvalidVerificationTokenException(token);
        }

        VerificationToken verificationToken = tokenOpt.get();
        // Eagerly fetch user if needed, or assume lazy loading works within transaction
        User user = verificationToken.getUser();
        String userIdStr = user != null ? String.valueOf(user.getUserId()) : "unknown"; // Handle potential null user?

        // Set audit context if user found
        if (user != null) {
            // Update audit context
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());

            // Set audit context
            String role = "USER"; // All authenticated users have USER role

            if (isAdmin) {
                role = "ADMIN"; // Add ADMIN role if user is in Admin table
            }
            userContextService.setUser(userIdStr, role);
        }

        if (verificationToken.isExpired()) {
            verificationToken.setStatus(VerificationToken.TokenStatus.EXPIRED);
            throw new ExpiredVerificationTokenException(token).withResendButton();
        }

        if (verificationToken.getStatus() != VerificationToken.TokenStatus.ACTIVE) {
            throw new InactiveTokenException(token, verificationToken.getStatus().toString());
        }


        // Optional: Check token type if tokens are used for multiple purposes
        if (verificationToken.getTokenType() != VerificationToken.TokenType.REGISTRATION) {
            throw new TokenTypeMismatchException(token, VerificationToken.TokenType.REGISTRATION, verificationToken.getTokenType());
        }

        assert user != null;
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        verificationToken.setStatus(VerificationToken.TokenStatus.USED);

        userRepository.save(user);
        verificationTokenRepository.save(verificationToken); // Save updated token status
    }
}